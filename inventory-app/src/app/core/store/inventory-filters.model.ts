export interface InventoryFilters {
  category: string | null;
}

export const DEFAULT_FILTERS: InventoryFilters = { category: null };

export const FILTERS_STORAGE_KEY = 'stockflow.inventory.filters';
