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

export function stockStatusOf(product: Pick<Product, 'currentStock' | 'minStock'>): StockStatus {
  if (product.currentStock <= product.minStock * 0.5) {
    return 'CRITICO';
  }
  if (product.currentStock <= product.minStock) {
    return 'BAJO';
  }
  return 'OK';
}
