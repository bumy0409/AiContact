import { apiFetch } from "../fetchClient";
import type { ApiResponse } from "../types/common";
import type {
  CreateComicStripsRequest,
  UpdateComicStripsTitleRequest,
} from "./request";
import type { ComicStripsListResponse, ComicStripsResponse } from "./response";

export const ComicStripsApi = {
  /* 커플 ID 기반으로 만화 생성 요청 */
  create: (payload: CreateComicStripsRequest) =>
    apiFetch<ApiResponse<ComicStripsResponse>>("/comic", {
      method: "POST",
      body: JSON.stringify(payload),
    }),

  /* 특정 만화의 제목 수정 */
  updateTitle: (id: number, payload: UpdateComicStripsTitleRequest) =>
    apiFetch<ApiResponse<ComicStripsResponse>>(`/comic/${id}/title`, {
      method: "PATCH",
      body: JSON.stringify(payload),
    }),

  /* 현재 커플의 모든 만화 조회 */
  getList: () =>
    apiFetch<ApiResponse<ComicStripsListResponse[]>>("/comic/list"),

  /* 만화 생성 상태 조회 */
  getStatus: (id: number) =>
    apiFetch<ApiResponse<{ status: string; imageUrl?: string; title?: string }>>(
      `/comic/${id}/status`
    ),

  /* 만화 삭제 (본인 커플 소유권 확인됨) */
  delete: (id: number) =>
    apiFetch<ApiResponse<string>>(`/comic/${id}`, {
      method: "DELETE",
    }),
};
