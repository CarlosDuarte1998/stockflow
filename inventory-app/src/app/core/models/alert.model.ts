export type AlertSeverity = 'LOW' | 'CRITICAL';

export interface StockAlert {
  productId: number;
  productName: string;
  currentStock: number;
  minStock: number;
  severity: AlertSeverity;
}
