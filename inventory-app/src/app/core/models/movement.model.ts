export type MovementType = 'IN' | 'OUT';

export interface Movement {
  id: number;
  productId: number;
  type: MovementType;
  quantity: number;
  reason: string | null;
  timestamp: string;
}

export interface MovementRequest {
  productId: number;
  type: MovementType;
  quantity: number;
  reason?: string;
}
