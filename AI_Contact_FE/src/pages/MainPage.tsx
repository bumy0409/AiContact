import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import BabyAvatar from "../components/BabyAvatar";
import ChatPanel from "../components/ChatPanel";
import EventCalendar from "../components/MainEventCalendar";
import RightIcons from "../components/RightIcons";
import Sidebar from "../components/Sidebar";
import Loading from "../components/animations/Loading";
import "../styles/MainPages.css";
import "../styles/UserInfo.css";

import { aiChildApi } from "../apis/aiChild";
import type { AiChildResponse } from "../apis/aiChild/response";
import { CouplesApi } from "../apis/couple";
import type {
  CoupleInfoResponse,
  PartnerInfoResponse,
} from "../apis/couple/response";
import { dailySchedulesApi } from "../apis/dailySchedule";
import type { DailyScheduleResponse } from "../apis/dailySchedule/response";
import { UsersApi } from "../apis/user/api";
import type { MeUserResponse } from "../apis/user/response";
import Particles from "../components/auth/Particles";

export default function MainPage() {
  const navigate = useNavigate();
  const [userInfo, setUserInfo] = useState<MeUserResponse | null>(null);
  const [partner, setPartner] = useState<PartnerInfoResponse | null>(null);
  const [coupleMeta, setCoupleMeta] = useState<CoupleInfoResponse | null>(null);
  const [dDay, setDday] = useState<DailyScheduleResponse[]>([]);
  const [isChatOpen, setIsChatOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [imgVersion, setImgVersion] = useState(0);
  const [growing, setGrowing] = useState(false);
  const [child, setChild] = useState<AiChildResponse | null>(null);

  useEffect(() => {
    let cancelled = false;

    const fetchAll = async () => {
      try {
        const [meRes, ddayRes] = await Promise.all([
          UsersApi.getMe(),
          dailySchedulesApi.getSchedulesDday(),
        ]);

        if (cancelled) return;

        setUserInfo(meRes.data);
        setDday(ddayRes.data);

        if (meRes.data?.coupleId) {
          try {
            const [partnerRes, coupleRes] = await Promise.all([
              CouplesApi.getPartnerInfo(),
              CouplesApi.getCoupleInfo(),
            ]);
            if (cancelled) return;
            setPartner(partnerRes.data);
            setCoupleMeta(coupleRes.data);
          } catch {
            setPartner(null);
            setCoupleMeta(null);
          }

          // 커플 상태라면 아이 정보도 조회
          try {
            const childRes = await aiChildApi.getMyChildren();
            if (!cancelled) setChild(childRes.data);
          } catch (e) {
            // 아이가 아직 없을 수 있음 → null 유지
            if (!cancelled) setChild(null);
          }
        } else {
          setPartner(null);
          setChild(null); // ✅ 커플이 아니면 아이 정보 없음
        }
      } catch (e) {
        console.error("[MainPage] fetch failed:", e);
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    fetchAll();
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!loading && userInfo && !partner) {
      navigate("/connection", { replace: true });
    }
  }, [loading, userInfo, partner, navigate]);

  const loveDays = useMemo(() => {
    if (!coupleMeta?.startDate) return null;
    try {
      const start = new Date(coupleMeta.startDate);
      const today = new Date();
      const diff = Math.floor(
        (Date.UTC(today.getFullYear(), today.getMonth(), today.getDate()) -
          Date.UTC(start.getFullYear(), start.getMonth(), start.getDate())) /
          (1000 * 60 * 60 * 24)
      );
      return diff + 1;
    } catch {
      return null;
    }
  }, [coupleMeta?.startDate]);

  return (
    <div className="main-layout">
      {loading ? <Loading /> : <></>}
      {growing && (
        <div className="loading-overlay">
          <Particles
            particleColors={["#735AE1", "#A66EE0", "#ffffff"]}
            particleCount={300}
            particleSpread={10}
            speed={0.2}
            particleBaseSize={1000}
            moveParticlesOnHover={true}
            alphaParticles={false}
            disableRotation={false}
            cameraDistance={10}
          />
          <Loading />
        </div>
      )}
      <Sidebar />
      <div className="main-content">
        <div className="page-header">
          <h4>
            {userInfo?.name}
            {partner?.name ? ` 💗 ${partner.name}` : ""}
          </h4>
          <h3>
            사랑한지 <span>{loveDays ?? ""}일</span> 째
          </h3>
        </div>

        <div className="content-row">
          <EventCalendar data={dDay} />
          <BabyAvatar
            name={child?.name || ""}
            imageUrl={child?.imageUrl || "Ai.png"}
            canGrow={
              !!child &&
              child.experiencePoints >= 500 &&
              child.growthLevel === 1
            }
            imgVersion={imgVersion}
            onGrowClick={async () => {
              if (!child) return;
              try {
                setGrowing(true);
                await aiChildApi.growChild(child.id);
                const updated = await aiChildApi.getMyChildren();
                setChild(updated.data);
                setImgVersion((v) => v + 1);
              } catch (err) {
                console.error("성장 실패:", err);
              } finally {
                setGrowing(false);
              }
            }}
            onRegenerateClick={child ? async () => {
              try {
                setGrowing(true);
                await aiChildApi.regenerateImage(child.id);
                const updated = await aiChildApi.getMyChildren();
                setChild(updated.data);
                setImgVersion((v) => v + 1);
              } catch (err) {
                console.error("이미지 재생성 실패:", err);
              } finally {
                setGrowing(false);
              }
            } : undefined}
            isProcessing={growing}
          />

          <div className="baby-stats">
            <div>
              나이 👼🏻
              <div className="baby-stats-content">
                {child ? `${Math.floor(child.experiencePoints / 100)}살` : "-"}
              </div>
            </div>
            <div>
              <div className="baby-stats-content-wrapper">
                <div>친밀도 💘</div>
                <div className="baby-stats-content-bar-percent">
                  {child ? `${child.experiencePoints % 100} / 100` : "- / 100"}
                </div>
              </div>
              <div className="baby-stats-content-bar">
                <div
                  className="baby-stats-content-bar-fill"
                  style={{
                    width: `${child ? child.experiencePoints % 100 : 0}%`,
                  }}
                />
              </div>
              {/* 수치 표시 */}
            </div>
          </div>
        </div>

        <RightIcons onChatClick={() => setIsChatOpen((v) => !v)} />

        {userInfo?.coupleId && (
          <ChatPanel
            isOpen={isChatOpen}
            onClose={() => setIsChatOpen(false)}
            coupleId={userInfo?.coupleId}
            senderId={userInfo?.id}
          />
        )}
      </div>
    </div>
  );
}
