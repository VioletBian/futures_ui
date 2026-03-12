type NotificationBannerProps = {
  kind: 'success' | 'danger' | 'warning';
  message: string;
};

export function NotificationBanner({ kind, message }: NotificationBannerProps) {
  return <div className={`notification-banner ${kind}`}>{message}</div>;
}