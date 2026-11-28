import axios from 'axios';

export type CatalogKind = 'movie' | 'tv' | 'anime';
export interface CatalogEntry {
  id: number;
  mediaType: 'movie' | 'tv';
  title: string;
  year: string;
  poster: string | null;
}
interface TmdbItem {
  id: number;
  adult?: boolean;
  title?: string;
  name?: string;
  release_date?: string;
  first_air_date?: string;
  poster_path?: string | null;
  genre_ids?: number[];
  original_language?: string;
  origin_country?: string[];
}
interface TmdbPage {results?: TmdbItem[]; total_pages?: number}

export async function fetchCatalogPage(kind: CatalogKind, query: string, page: number, apiKey: string, signal?: AbortSignal): Promise<{items: CatalogEntry[]; nextPage?: number}> {
  const mediaType = kind === 'movie' ? 'movie' : 'tv';
  const isSearch = Boolean(query.trim());
  const params: Record<string, string | number | boolean> = {
    api_key: apiKey, page, language: 'id-ID', include_adult: false,
  };
  if (isSearch) params.query = query.trim();
  else {
    params.sort_by = 'popularity.desc';
    if (kind === 'anime') {
      params.with_genres = '16';
      params.with_origin_country = 'JP';
    }
  }
  const {data} = await axios.get<TmdbPage>(`https://api.themoviedb.org/3/${isSearch ? 'search' : 'discover'}/${mediaType}`, {params, signal, timeout: 15000});
  const items = (data.results ?? []).filter(item => {
    if (item.adult === true || !Number.isFinite(item.id) || !(item.title || item.name)) return false;
    if (kind === 'anime') return item.genre_ids?.includes(16) && (item.original_language === 'ja' || item.origin_country?.includes('JP'));
    return true;
  }).map(item => ({
    id: item.id,
    mediaType,
    title: (item.title || item.name)!,
    year: (item.release_date || item.first_air_date || '').slice(0, 4),
    poster: item.poster_path ? `https://image.tmdb.org/t/p/w342${item.poster_path}` : null,
  }));
  return {items, nextPage: page < Math.min(data.total_pages ?? page, 500) ? page + 1 : undefined};
}
