import { useNavigate } from "react-router-dom";
import TalkIcon from "../assets/icons/TalkIcon.svg?react";
import "../styles/BabyAvatar.css";

interface BabyAvatarProps {
  name: string;
  imageUrl: string;
  canGrow?: boolean;
  onGrowClick?: () => void;
  onRegenerateClick?: () => void;
  imgVersion?: number;
  isProcessing?: boolean;
}

export default function BabyAvatar({
  name,
  imageUrl,
  canGrow = false,
  onGrowClick,
  onRegenerateClick,
  imgVersion = 1,
  isProcessing = false,
}: BabyAvatarProps) {
  const navigate = useNavigate();

  const cacheBustedSrc =
    imageUrl + (imageUrl.includes("?") ? "&" : "?") + `v=${imgVersion}`;

  return (
    <div className="baby-container">
      {canGrow ? <div className="grow-wrapper"></div> : <></>}
      <div className="baby-avatar-wrapper">
        <h1 className="baby-name">{name}</h1>

        <div className="image-wrapper">
          <img
            key={imgVersion}
            src={cacheBustedSrc}
            alt={name}
            className="baby-image"
          />

          {onRegenerateClick && (
            <button
              className={`talk-button talk-button--left ${isProcessing ? "disabled" : ""}`}
              onClick={isProcessing ? undefined : onRegenerateClick}
              disabled={isProcessing}
            >
              {isProcessing ? "생성 중..." : "아이 재탄생"}
            </button>
          )}

          {canGrow ? (
            <button
              className={`talk-button grow-button ${isProcessing ? "disabled" : ""}`}
              onClick={isProcessing ? undefined : onGrowClick}
              disabled={isProcessing}
            >
              {isProcessing ? "성장 중..." : "🌱 성장하기"}
            </button>
          ) : (
            <div className="talk-button" onClick={() => navigate("/talk")}>
              <TalkIcon />
              <div>이야기하기</div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
