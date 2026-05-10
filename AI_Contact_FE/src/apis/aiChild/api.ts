import { apiFetch } from "../fetchClient";
import type { ApiResponse } from "../types/common";
import type { UpdateAiChildRequest, CreateAiChildRequest } from "./request";
import type { AiChildResponse } from "./response";

const BASE_PATH = "/children";

export const aiChildApi = {
  createChild: (payload: CreateAiChildRequest) =>
    apiFetch<ApiResponse<AiChildResponse>>(BASE_PATH, {
      method: "POST",
      body: JSON.stringify(payload), // 아이 이름 전달
    }),

  getMyChildren: () => apiFetch<ApiResponse<AiChildResponse>>(BASE_PATH),

  updateChild: (id: number, payload: Partial<UpdateAiChildRequest>) =>
    apiFetch<ApiResponse<AiChildResponse>>(`${BASE_PATH}/${id}`, {
      method: "PUT",
      body: JSON.stringify(payload),
    }),

  /* 아이 성장 */
  growChild: (id: number) =>
    apiFetch<ApiResponse<AiChildResponse>>(`${BASE_PATH}/${id}/grow`, {
      method: "GET",
    }),

  regenerateImage: (id: number) =>
    apiFetch<ApiResponse<AiChildResponse>>(`${BASE_PATH}/${id}/regenerate-image`, {
      method: "POST",
    }),

  deleteChild: (id: number) =>
    apiFetch<ApiResponse<string>>(`${BASE_PATH}/${id}`, {
      method: "DELETE",
    }),
};
