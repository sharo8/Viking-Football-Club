import type { Metadata } from "next";
import { Cinzel, Cinzel_Decorative, Inter, Work_Sans, JetBrains_Mono } from "next/font/google";
import "./globals.css";
import { ThemeProvider } from "@/components/providers/ThemeProvider";
import { LocaleProvider } from "@/components/providers/LocaleProvider";
import { AuthProvider } from "@/components/providers/AuthProvider";
import { QueryProvider } from "@/components/providers/QueryProvider";
import { SiteChrome } from "@/components/layout/SiteChrome";

const cinzel = Cinzel({
  subsets: ["latin"],
  weight: ["500", "600", "700"],
  variable: "--font-cinzel",
});

const cinzelDecorative = Cinzel_Decorative({
  subsets: ["latin"],
  weight: ["700"],
  variable: "--font-cinzel-decorative",
});

const workSans = Work_Sans({
  subsets: ["latin"],
  variable: "--font-work-sans",
});

const jetbrains = JetBrains_Mono({
  subsets: ["latin"],
  variable: "--font-jetbrains",
});

const inter = Inter({
  subsets: ["latin"],
  display: "swap",
  variable: "--font-inter",
});

export const metadata: Metadata = {
  title: {
    default: "Viking FC | One Club. One Family. One Dream.",
    template: "%s | Viking FC",
  },
  description:
    "Official website of Viking FC — professional football club. Matches, players, news, academy and shop.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" suppressHydrationWarning data-scroll-behavior="smooth">
      <head>
        <script
          dangerouslySetInnerHTML={{
            __html: `(function(){try{var t=localStorage.getItem("vfc_theme")||localStorage.getItem("viking-theme");if(t!=="dark"&&t!=="light"){t=matchMedia("(prefers-color-scheme: dark)").matches?"dark":"light";}document.documentElement.classList.toggle("dark",t==="dark");document.documentElement.dataset.theme=t;document.documentElement.style.colorScheme=t;var l=localStorage.getItem("vfc_lang")||localStorage.getItem("viking-locale");if(l)document.documentElement.lang=l;}catch(e){}})();`,
          }}
        />
      </head>
      <body
        className={`${cinzel.variable} ${cinzelDecorative.variable} ${workSans.variable} ${jetbrains.variable} ${inter.variable} antialiased`}
      >
        <ThemeProvider>
          <LocaleProvider>
            <QueryProvider>
              <AuthProvider>
                <SiteChrome>{children}</SiteChrome>
              </AuthProvider>
            </QueryProvider>
          </LocaleProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}
