export interface Product {
  id: number;
  sku: string;
  name: string;
  category: string;
  currentStock: number;
  minStock: number;
  unitPrice: number;
}

export type StockStatus = 'OK' | 'BAJO' | 'CRITICO';

export function estadoStockDe(producto: Pick<Product, 'currentStock' | 'minStock'>): StockStatus {
  if (producto.currentStock <= producto.minStock * 0.5) {
    return 'CRITICO';
  }
  if (producto.currentStock <= producto.minStock) {
    return 'BAJO';
  }
  return 'OK';
}
