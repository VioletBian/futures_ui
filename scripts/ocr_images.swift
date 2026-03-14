#!/usr/bin/swift

import CoreGraphics
import Foundation
import ImageIO
import Vision

struct OCRLine {
    let text: String
    let minX: CGFloat
    let maxY: CGFloat
}

enum OCRToolError: Error, CustomStringConvertible {
    case missingPath(String)
    case unreadableImage(String)
    case failedRecognition(String)

    var description: String {
        switch self {
        case .missingPath(let path):
            return "Path not found: \(path)"
        case .unreadableImage(let path):
            return "Unable to read image: \(path)"
        case .failedRecognition(let path):
            return "Text recognition failed: \(path)"
        }
    }
}

func usage() {
    let message = """
    Usage: ocr_images.swift <path> [<path> ...]

    Accepts image files or directories. Directories are scanned recursively for:
    .jpg, .jpeg, .png, .webp
    """
    FileHandle.standardError.write(Data(message.utf8))
}

func expandPaths(_ rawPaths: [String]) throws -> [String] {
    let fileManager = FileManager.default
    var images: [String] = []

    for rawPath in rawPaths {
        let path = NSString(string: rawPath).expandingTildeInPath
        var isDirectory: ObjCBool = false
        guard fileManager.fileExists(atPath: path, isDirectory: &isDirectory) else {
            throw OCRToolError.missingPath(path)
        }

        if isDirectory.boolValue {
            let enumerator = fileManager.enumerator(
                at: URL(fileURLWithPath: path),
                includingPropertiesForKeys: [.isRegularFileKey],
                options: [.skipsHiddenFiles]
            )

            while let url = enumerator?.nextObject() as? URL {
                let ext = url.pathExtension.lowercased()
                if ["jpg", "jpeg", "png", "webp"].contains(ext) {
                    images.append(url.path)
                }
            }
        } else {
            images.append(path)
        }
    }

    return images.sorted()
}

func groupLines(_ observations: [VNRecognizedTextObservation]) -> [String] {
    let items: [OCRLine] = observations.compactMap { observation in
        guard let candidate = observation.topCandidates(1).first else {
            return nil
        }

        let box = observation.boundingBox
        return OCRLine(
            text: candidate.string.trimmingCharacters(in: .whitespacesAndNewlines),
            minX: box.minX,
            maxY: box.maxY
        )
    }.filter { !$0.text.isEmpty }

    let sorted = items.sorted {
        if abs($0.maxY - $1.maxY) > 0.015 {
            return $0.maxY > $1.maxY
        }
        return $0.minX < $1.minX
    }

    var grouped: [[OCRLine]] = []
    for item in sorted {
        if let lastIndex = grouped.indices.last {
            let referenceY = grouped[lastIndex].map(\.maxY).reduce(0, +) / CGFloat(grouped[lastIndex].count)
            if abs(referenceY - item.maxY) <= 0.015 {
                grouped[lastIndex].append(item)
                continue
            }
        }
        grouped.append([item])
    }

    return grouped.map { line in
        line.sorted { $0.minX < $1.minX }
            .map(\.text)
            .joined(separator: " ")
    }
}

func recognizeText(in imagePath: String) throws -> [String] {
    guard
        let source = CGImageSourceCreateWithURL(URL(fileURLWithPath: imagePath) as CFURL, nil),
        let cgImage = CGImageSourceCreateImageAtIndex(source, 0, nil)
    else {
        throw OCRToolError.unreadableImage(imagePath)
    }

    let request = VNRecognizeTextRequest()
    request.recognitionLevel = .accurate
    request.usesLanguageCorrection = false
    request.recognitionLanguages = ["en-US"]
    request.minimumTextHeight = 0.01

    let handler = VNImageRequestHandler(cgImage: cgImage, options: [:])
    do {
        try handler.perform([request])
    } catch {
        throw OCRToolError.failedRecognition("\(imagePath): \(error.localizedDescription)")
    }

    let observations = request.results ?? []
    return groupLines(observations)
}

let args = Array(CommandLine.arguments.dropFirst())
guard !args.isEmpty else {
    usage()
    exit(1)
}

do {
    let paths = try expandPaths(args)
    for (index, path) in paths.enumerated() {
        let lines = try recognizeText(in: path)
        print("===== \(path) =====")
        if lines.isEmpty {
            print("[no text recognized]")
        } else {
            lines.forEach { print($0) }
        }
        if index < paths.count - 1 {
            print("")
        }
    }
} catch {
    FileHandle.standardError.write(Data("\(error)\n".utf8))
    exit(1)
}
