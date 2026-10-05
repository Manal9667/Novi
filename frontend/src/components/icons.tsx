import React from 'react';

/**
 * A small, consistent set of hand-drawn-feeling line icons and spot
 * illustrations, all inline SVG so they inherit `currentColor` and theme
 * automatically. Icons use a 24px grid; illustrations are larger scenes used
 * for empty states and the landing page.
 */

type IconProps = {
  size?: number;
  className?: string;
  title?: string;
};

function base(size = 24, className?: string, title?: string) {
  return {
    width: size,
    height: size,
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 1.7,
    strokeLinecap: 'round' as const,
    strokeLinejoin: 'round' as const,
    className,
    role: title ? ('img' as const) : ('presentation' as const),
    'aria-hidden': title ? undefined : true,
    'aria-label': title
  };
}

export const BookIcon = ({ size, className, title }: IconProps) => (
  <svg {...base(size, className, title)}>
    <path d="M4 5.5A1.5 1.5 0 0 1 5.5 4H19a1 1 0 0 1 1 1v13.5" />
    <path d="M6 4v16" />
    <path d="M20 18.5a2.5 2.5 0 0 0-2.5-2.5H6a2 2 0 0 0-2 2v0A1.5 1.5 0 0 0 5.5 20H18" />
  </svg>
);

export const CompassIcon = ({ size, className, title }: IconProps) => (
  <svg {...base(size, className, title)}>
    <circle cx="12" cy="12" r="9" />
    <path d="M15.5 8.5 13 13l-4.5 2.5L11 11z" />
  </svg>
);

export const SparkleIcon = ({ size, className, title }: IconProps) => (
  <svg {...base(size, className, title)}>
    <path d="M12 3c.4 3.5 1.5 4.6 5 5-3.5.4-4.6 1.5-5 5-.4-3.5-1.5-4.6-5-5 3.5-.4 4.6-1.5 5-5Z" />
    <path d="M18.5 14c.2 1.6.7 2.1 2.3 2.3-1.6.2-2.1.7-2.3 2.3-.2-1.6-.7-2.1-2.3-2.3 1.6-.2 2.1-.7 2.3-2.3Z" />
  </svg>
);

export const ShelfIcon = ({ size, className, title }: IconProps) => (
  <svg {...base(size, className, title)}>
    <path d="M4 6v10M8 6v10M12 7l3-.5 1.5 9.5-3 .5z" />
    <path d="M3 20h18" />
  </svg>
);

export const SearchIcon = ({ size, className, title }: IconProps) => (
  <svg {...base(size, className, title)}>
    <circle cx="11" cy="11" r="6.5" />
    <path d="m16 16 4.5 4.5" />
  </svg>
);

export const CameraIcon = ({ size, className, title }: IconProps) => (
  <svg {...base(size, className, title)}>
    <path d="M4 8.5A1.5 1.5 0 0 1 5.5 7h2L9 5h6l1.5 2h2A1.5 1.5 0 0 1 20 8.5V17a1.5 1.5 0 0 1-1.5 1.5h-13A1.5 1.5 0 0 1 4 17z" />
    <circle cx="12" cy="12.5" r="3.2" />
  </svg>
);

export const ClockIcon = ({ size, className, title }: IconProps) => (
  <svg {...base(size, className, title)}>
    <circle cx="12" cy="12" r="9" />
    <path d="M12 7.5V12l3 2" />
  </svg>
);

export const UserIcon = ({ size, className, title }: IconProps) => (
  <svg {...base(size, className, title)}>
    <circle cx="12" cy="8.5" r="3.5" />
    <path d="M5.5 19.5a6.5 6.5 0 0 1 13 0" />
  </svg>
);

export const GearIcon = ({ size, className, title }: IconProps) => (
  <svg {...base(size, className, title)}>
    <circle cx="12" cy="12" r="3" />
    <path d="M12 2.5v2.2M12 19.3v2.2M4.2 7l1.9 1.1M17.9 15.9 19.8 17M4.2 17l1.9-1.1M17.9 8.1 19.8 7" />
  </svg>
);

export const HomeIcon = ({ size, className, title }: IconProps) => (
  <svg {...base(size, className, title)}>
    <path d="M4 11 12 4l8 7" />
    <path d="M6 9.5V20h12V9.5" />
  </svg>
);

export const MoonIcon = ({ size, className, title }: IconProps) => (
  <svg {...base(size, className, title)}>
    <path d="M20 13.5A8 8 0 1 1 10.5 4a6.5 6.5 0 0 0 9.5 9.5Z" />
  </svg>
);

export const SunIcon = ({ size, className, title }: IconProps) => (
  <svg {...base(size, className, title)}>
    <circle cx="12" cy="12" r="4" />
    <path d="M12 2.5v2M12 19.5v2M2.5 12h2M19.5 12h2M5 5l1.5 1.5M17.5 17.5 19 19M5 19l1.5-1.5M17.5 6.5 19 5" />
  </svg>
);

export const MenuIcon = ({ size, className, title }: IconProps) => (
  <svg {...base(size, className, title)}>
    <path d="M4 7h16M4 12h16M4 17h16" />
  </svg>
);

export const BookmarkIcon = ({ size, className, title }: IconProps) => (
  <svg {...base(size, className, title)}>
    <path d="M7 4h10v16l-5-3.5L7 20z" />
  </svg>
);

export const StarIcon = ({ size, className, title }: IconProps) => (
  <svg {...base(size, className, title)}>
    <path d="M12 3.5l2.6 5.3 5.9.9-4.3 4.1 1 5.8L12 17.9 6.8 19.6l1-5.8L3.5 9.7l5.9-.9z" />
  </svg>
);

/* ------------------------------- Illustrations ------------------------------ */

type IlloProps = { className?: string };

/** A warm reading-nook scene: lamp, stacked books, a steaming cup, a plant. */
export const NookIllustration = ({ className }: IlloProps) => (
  <svg className={className} viewBox="0 0 240 160" fill="none" role="img" aria-label="A cozy reading nook"
       stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
    {/* floor line */}
    <path d="M12 138h216" opacity="0.5" />
    {/* lamp */}
    <path d="M196 138V86" />
    <path d="M196 86l-14 8M196 86l14 8" opacity="0.6" />
    <path d="M180 70h32l-6 16h-20z" fill="var(--gold)" stroke="var(--gold)" opacity="0.9" />
    <circle cx="196" cy="92" r="2.4" fill="currentColor" stroke="none" opacity="0.5" />
    {/* stacked books */}
    <rect x="28" y="118" width="86" height="20" rx="3" fill="var(--accent-soft)" />
    <rect x="36" y="98" width="78" height="20" rx="3" fill="var(--secondary-soft)" />
    <rect x="24" y="78" width="70" height="20" rx="3" fill="var(--accent-soft)" />
    {/* open book on top */}
    <path d="M58 78c6-5 16-5 22 0 6-5 16-5 22 0v-2c-6-5-16-5-22 0-6-5-16-5-22 0z"
          fill="var(--surface-raised)" />
    {/* tea cup */}
    <path d="M126 128h26v6a8 8 0 0 1-8 8h-10a8 8 0 0 1-8-8z" fill="var(--surface-raised)" />
    <path d="M152 130h5a4 4 0 0 1 0 8h-5" />
    <path d="M134 120c-1-3 1-4 0-7M142 120c-1-3 1-4 0-7" opacity="0.6" />
    {/* little plant */}
    <path d="M168 138v-10" />
    <path d="M168 130c-6 0-9-4-9-8 5 0 9 3 9 8zM168 132c6 0 9-4 9-9-5 0-9 4-9 9z"
          fill="var(--secondary-soft)" />
  </svg>
);

/** An empty wooden shelf — used for empty library/shelf states. */
export const EmptyShelfIllustration = ({ className }: IlloProps) => (
  <svg className={className} viewBox="0 0 220 140" fill="none" role="img" aria-label="An empty bookshelf"
       stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
    <rect x="24" y="34" width="172" height="76" rx="4" />
    <path d="M24 72h172" />
    <path d="M40 110v10M180 110v10" opacity="0.5" />
    {/* one lonely leaning book */}
    <path d="M58 50h12v22H58z" fill="var(--accent-soft)" transform="rotate(-8 64 61)" />
    <path d="M150 80h10v26h-10z" fill="var(--secondary-soft)" />
  </svg>
);

/** A book that has wandered off — the 404 scene. */
export const LostBookIllustration = ({ className }: IlloProps) => (
  <svg className={className} viewBox="0 0 220 150" fill="none" role="img" aria-label="A book that wandered off the shelf"
       stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M70 60c10-8 26-8 36 0 10-8 26-8 36 0v46c-10-8-26-8-36 0-10-8-26-8-36 0z"
          fill="var(--surface-raised)" />
    <path d="M106 60v46" opacity="0.5" />
    <path d="M44 118h132" opacity="0.4" />
    {/* little footprints wandering away */}
    <path d="M30 108c2 0 3 1 3 3M22 98c2 0 3 1 3 3M14 90c2 0 3 1 3 3" opacity="0.6" />
  </svg>
);
