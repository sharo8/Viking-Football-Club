import Link from "next/link";

export default function NotFound() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-navy-deep px-4 text-center text-white">
      <p className="font-display text-8xl text-red">404</p>
      <h1 className="mt-2 font-display text-4xl tracking-wider">Page Not Found</h1>
      <Link href="/" className="btn-primary mt-8">
        Back Home
      </Link>
    </div>
  );
}
