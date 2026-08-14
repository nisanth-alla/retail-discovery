const AVATAR_IDLE = `${import.meta.env.BASE_URL}models/maleAvatarT.png`;
const AVATAR_SPEAKING = `${import.meta.env.BASE_URL}models/maleAvatarT.gif`;

type Avatar2DProps = {
  isSpeaking?: boolean;
};

export function Avatar2D({ isSpeaking = false }: Avatar2DProps) {
  return (
    <div className="relative w-full overflow-hidden rounded-2xl bg-[#0f172a]" style={{ aspectRatio: "4/5", maxWidth: 400, margin: "0 auto" }}>
      <img
        src={isSpeaking ? AVATAR_SPEAKING : AVATAR_IDLE}
        alt="Human Avatar"
        className="block h-full w-full rounded-2xl object-cover"
        draggable={false}
      />
    </div>
  );
}
