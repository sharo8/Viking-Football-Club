/** @type {import('next').NextConfig} */
const nextConfig = {
  async redirects() {
    return [{ source: "/admin/users", destination: "/admin/administration", permanent: false }];
  },
  images: {
    unoptimized: false,
  },
  // Hide the Next.js "N" dev badge
  devIndicators: false,
};

export default nextConfig;
