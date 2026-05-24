import { useRef, useState } from "react";
import { createPortal } from "react-dom";
import HTMLFlipBook from "react-pageflip";
import { useNavigate } from "react-router-dom";
import type { ComicStripsListResponse } from "../apis/comicStrips";
import { ComicStripsApi } from "../apis/comicStrips";
import backgroundImage from "../assets/images/Cartoon.png";
import Loading from "../components/animations/Loading";
import Particles from "../components/auth/Particles.tsx";
import Cartoon from "../components/cartoon/Cartoon";
import Modal from "../components/modal/Modal";
import Sidebar from "../components/Sidebar";
import "../styles/Cartoon.css";
import "../styles/CartoonPage.css";
import "../styles/MainPages.css";

// FlipBook ref에서 필요한 메서드 shape만 정의
type FlipbookRef = {
  pageFlip(): {
    flipNext(): void;
    flipPrev(): void;
    turnToPage: (index: number) => void;
  };
};

export default function CartoonPage() {
  const navigate = useNavigate();

  // 생성 폼
  const [location, setLocation] = useState("");
  const [activity, setActivity] = useState("");
  const [weather, setWeather] = useState("");

  // 로딩/모달 상태
  const [isLoading, setIsLoading] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);

  // 이전 작품
  const [comicList, setComicList] = useState<ComicStripsListResponse[]>([]);
  const [currentPage, setCurrentPage] = useState(0);
  const bookRef = useRef<FlipbookRef | null>(null);

  const totalPages = Math.max(comicList.length, 1);
  const canPrev = currentPage > 0;
  const canNext = currentPage < totalPages - 1;

  // 책장 넘김 제어
  const flipPrev = () => bookRef.current?.pageFlip().flipPrev();
  const flipNext = () => bookRef.current?.pageFlip().flipNext();

  // 만화 생성
  const handleCreateComic = async () => {
    if (!location || !activity || !weather) {
      alert("모든 항목을 입력해주세요!");
      return;
    }

    try {
      setIsLoading(true);
      const res = await ComicStripsApi.create({ location, activity, weather });
      const comicId = res.data.id;

      // 3초마다 상태 폴링
      const poll = async () => {
        try {
          const statusRes = await ComicStripsApi.getStatus(comicId);
          const { status, imageUrl } = statusRes.data;

          if (status === "DONE" && imageUrl) {
            navigate("/cartoon-result", {
              state: { imageUrl, id: comicId },
            });
          } else if (status === "FAILED") {
            alert("만화 생성 중 오류가 발생했습니다.");
            setIsLoading(false);
          } else {
            setTimeout(poll, 3000);
          }
        } catch {
          alert("상태 확인 중 오류가 발생했습니다.");
          setIsLoading(false);
        }
      };

      setTimeout(poll, 3000);
    } catch (error) {
      console.error(error);
      alert("만화 생성 요청 중 오류가 발생했습니다.");
      setIsLoading(false);
    }
  };

  // 이전 작품 로딩
  const fetchPreviousComics = async () => {
    try {
      const res = await ComicStripsApi.getList();
      const list = res.data || [];
      setComicList(list);
      setCurrentPage(0);
      setIsModalOpen(true);
      // 안전하게 첫 페이지로 이동
      requestAnimationFrame(() => {
        bookRef.current?.pageFlip().turnToPage(0);
      });
    } catch (error) {
      console.error("만화 목록 로딩 실패", error);
      alert("이전 작품을 불러오는 데 실패했습니다.");
    }
  };

  return (
    <div className="main-layout">
      {/* 로딩 오버레이 */}
      {isLoading && (
        <div className="loading-background">
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

      {/* 책 넘김 모달 (화살표 UI를 DictionaryPage와 동일하게 구성) */}
      {isModalOpen &&
        createPortal(
          <Modal
            onClose={() => setIsModalOpen(false)}
            hasPrev={false} // 모달 내장 화살표 숨김
            hasNext={false}
          >
            <div className="dictionary-container" style={{ gap: 12 }}>
              {/* 좌측 화살표 (DictionaryPage와 동일한 마크업/클래스) */}
              <button
                className="arrow left arrow-white"
                onClick={flipPrev}
                disabled={!canPrev}
                aria-label="이전 페이지"
              >
                〈
              </button>

              {/* 책 본문 */}
              <div className="dictionary-book">
                {/* 책 모양의 더미 페이지 2장 (그림자/깊이감용) - 선택사항 */}
                <div className="dictionary-page-mock">
                  <div className="flip-page mock-page"></div>
                  <div className="flip-page mock-page"></div>
                </div>

                <HTMLFlipBook
                  ref={bookRef as any}
                  className="flipbook"
                  style={{}} // 타입 요구로 필수
                  width={734 / 2} // 한쪽 페이지 너비
                  height={467} // 페이지 높이
                  size="stretch"
                  minWidth={320}
                  maxWidth={1000}
                  minHeight={420}
                  maxHeight={1400}
                  startPage={0}
                  flippingTime={700}
                  startZIndex={10}
                  drawShadow={true}
                  maxShadowOpacity={0.3}
                  autoSize={true}
                  showCover={false}
                  mobileScrollSupport={true}
                  usePortrait={true}
                  useMouseEvents={false}
                  swipeDistance={30}
                  clickEventForward={true}
                  showPageCorners={false} // 타입 충돌 회피용 필수 prop
                  disableFlipByClick={true} // 타입 충돌 회피용 필수 prop
                  onFlip={(e: any) => setCurrentPage(e.data)}
                >
                  {comicList.length === 0 ? (
                    <div key="empty" className="flip-page cartoon-page">
                      <div className="cartoon-detail">
                        <div className="empty-text">
                          아직 생성된 만화가 없습니다.
                        </div>
                      </div>
                    </div>
                  ) : (
                    comicList.map((item, idx) => (
                      <div
                        key={`cartoon-${idx}`}
                        className="flip-page cartoon-page"
                      >
                        <div className="cartoon-detail">
                          <Cartoon
                            date={new Date(item.createdAt)}
                            image_url={item.imageUrl}
                            title={item.title ?? ""}
                          />
                        </div>
                      </div>
                    ))
                  )}
                  {/* 마지막 안내 페이지(옵션) */}
                  <div key="tail" className="flip-page cartoon-page">
                    <div className="cartoon-detail">
                      <div className="empty-text">
                        ✨ 새로운 이미지를 생성해보세요.
                      </div>
                    </div>
                  </div>
                </HTMLFlipBook>
              </div>

              {/* 우측 화살표 (DictionaryPage와 동일한 마크업/클래스) */}
              <button
                className="arrow right arrow-white"
                onClick={flipNext}
                disabled={!canNext}
                aria-label="다음 페이지"
              >
                〉
              </button>
            </div>
          </Modal>,
          document.body
        )}

      {/* 본문 */}
      <Sidebar />

      <div className="cartoon-content">
        <img src={backgroundImage} alt="배경" className="background-img" />

        <div
          className="back-ai"
          onClick={() => navigate("/ai")}
          role="button"
          tabIndex={0}
        >
          ←
        </div>

        <div className="page-header page-header-light">
          <h4># 재미있는 # 네컷만화</h4>
          <h3>네컷만화 제작소</h3>
        </div>

        <div className="board-box">
          <div className="board-box-title">
            <h3>한 단어 또는 짧은 문장으로 오늘 하루를 표현해주세요!</h3>
            <h3>그럼 제가 4컷 만화로 표현해 드릴게요!</h3>
          </div>

          <div className="input-overlay">
            <div className="answer-input-title">
              장소
              <input
                type="text"
                className="answer-input input-1"
                placeholder="예: 제주도 / 놀이공원 / 서울숲"
                value={location}
                onChange={(e) => setLocation(e.target.value)}
              />
            </div>
            <div className="answer-input-title">
              활동
              <input
                type="text"
                className="answer-input input-2"
                placeholder="예: 해변 산책 / 영화보기 / 떡볶이"
                value={activity}
                onChange={(e) => setActivity(e.target.value)}
              />
            </div>
            <div className="answer-input-title">
              날씨 또는 계절
              <input
                type="text"
                className="answer-input input-3"
                placeholder="예: 맑음 / 여름"
                value={weather}
                onChange={(e) => setWeather(e.target.value)}
              />
            </div>
          </div>

          <div className="button-container">
            <button className="page-btn" onClick={fetchPreviousComics}>
              이전 작품
            </button>
            <button
              className="page-btn"
              onClick={handleCreateComic}
              disabled={isLoading}
            >
              {isLoading ? "생성 중..." : "제작하기"}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
