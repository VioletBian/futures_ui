#!/bin/bash

# 配置区域
BASE_DIR="/opt/user/biaash"
OLD_TOKEN="YOUR_OLD_TOKEN"  # 替换为你的旧token
NEW_TOKEN="YOUR_NEW_TOKEN"  # 替换为你的新token

# 遍历所有子目录
for dir in "$BASE_DIR"/*; do
    if [ -d "$dir/.git" ]; then
        echo "处理项目: $(basename "$dir")"
        cd "$dir" || continue

        # 获取当前 origin URL
        current_url=$(git remote get-url origin 2>/dev/null)

        if [ -n "$current_url" ]; then
            echo "  当前 URL: $current_url"

            # 替换 token
            new_url=$(echo "$current_url" | sed "s/${OLD_TOKEN}/${NEW_TOKEN}/g")

            if [ "$current_url" != "$new_url" ]; then
                # 设置新 URL
                git remote set-url origin "$new_url"
                echo "  ✓ 已更新为: $new_url"
            else
                echo "  - 未找到旧token，跳过"
            fi
        else
            echo "  - 没有 origin remote"
        fi
        echo ""
    fi
done

echo "完成！"
