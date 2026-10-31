"use client";

import { useParams } from "next/navigation";
import { SectionView } from "@/components/admin/SectionView";

export default function AdminSectionPage() {
  const params = useParams<{ section: string }>();
  return <SectionView section={params.section} />;
}
