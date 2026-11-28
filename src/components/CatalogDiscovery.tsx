import React, {useEffect, useMemo, useState} from 'react';
import {ActivityIndicator, FlatList, Image, Linking, Pressable, View} from 'react-native';
import {useInfiniteQuery} from '@tanstack/react-query';
import {useIsFocused} from '@react-navigation/native';
import AppText from './ui/Text';
import {useM3Colors} from '../theme/M3PaletteContext';
import {getTmdbApiKey} from '../lib/hooks/useTmdbStory';
import {fetchCatalogPage, type CatalogKind, type CatalogEntry} from '../lib/services/catalogDiscovery';

const categories: {id: CatalogKind; label: string}[] = [
  {id: 'movie', label: 'Film'},
  {id: 'tv', label: 'Serial'},
  {id: 'anime', label: 'Anime'},
];

export default function CatalogDiscovery({query}: {query: string}) {
  const colors = useM3Colors();
  const [kind, setKind] = useState<CatalogKind>('movie');
  const [text, setText] = useState(query.trim());
  const focused = useIsFocused();
  const apiKey = getTmdbApiKey();
  useEffect(() => {
    const timeout = setTimeout(() => setText(query.trim()), 300);
    return () => clearTimeout(timeout);
  }, [query]);

  const result = useInfiniteQuery({
    queryKey: ['yuuzprime-catalog', kind, text, apiKey],
    initialPageParam: 1,
    queryFn: ({pageParam, signal}) => fetchCatalogPage(kind, text, pageParam, apiKey, signal),
    getNextPageParam: last => last.nextPage,
    enabled: focused && Boolean(apiKey),
    staleTime: 5 * 60 * 1000,
    retry: 1,
  });
  const entries = useMemo(() => {
    const unique = new Map<string, CatalogEntry>();
    for (const page of result.data?.pages ?? []) {
      for (const item of page.items) unique.set(`${item.mediaType}-${item.id}`, item);
    }
    return Array.from(unique.values());
  }, [result.data]);
  const openDetails = (item: CatalogEntry) => {
    Linking.openURL(`https://www.themoviedb.org/${item.mediaType}/${item.id}`).catch(() => {});
  };

  return (
    <View style={{flex: 1}}>
      <View style={{flexDirection: 'row', gap: 8, paddingHorizontal: 16, marginBottom: 10}}>
        {categories.map(category => (
          <Pressable key={category.id} accessibilityRole="button" accessibilityState={{selected: kind === category.id}} onPress={() => setKind(category.id)} style={{flex: 1, alignItems: 'center', paddingVertical: 11, borderRadius: 18, backgroundColor: kind === category.id ? colors.primaryContainer : colors.surfaceContainerLow}}>
            <AppText role="labelLargeEmphasized" style={{color: kind === category.id ? colors.onPrimaryContainer : colors.onSurface}}>{category.label}</AppText>
          </Pressable>
        ))}
      </View>
      <AppText role="bodySmall" style={{color: colors.onSurfaceVariant, marginHorizontal: 16, marginBottom: 10}}>
        {text ? 'Hasil pencarian katalog' : 'Jelajahi judul populer'} · Metadata TMDB, bukan sumber video.
      </AppText>
      <FlatList
        key={kind + ':' + text}
        data={entries}
        keyExtractor={item => `${item.mediaType}-${item.id}`}
        numColumns={2}
        contentContainerStyle={{paddingHorizontal: 12, paddingBottom: 120, flexGrow: 1}}
        keyboardShouldPersistTaps="handled"
        showsVerticalScrollIndicator={false}
        renderItem={({item}) => (
          <Pressable accessibilityRole="link" accessibilityLabel={`Informasi ${item.title} di TMDB`} onPress={() => openDetails(item)} style={{flex: 1, maxWidth: '50%', padding: 5}}>
            <View style={{borderRadius: 16, overflow: 'hidden', backgroundColor: colors.surfaceContainer}}>
              {item.poster ? <Image source={{uri: item.poster}} resizeMode="cover" style={{width: '100%', aspectRatio: 2 / 3}} /> : <View style={{width: '100%', aspectRatio: 2 / 3, justifyContent: 'center', alignItems: 'center', padding: 12}}><AppText style={{color: colors.onSurfaceVariant}}>Poster belum tersedia</AppText></View>}
              <View style={{padding: 10}}>
                <AppText role="bodyLargeEmphasized" numberOfLines={2} style={{color: colors.onSurface}}>{item.title}</AppText>
                <AppText role="bodySmall" style={{color: colors.onSurfaceVariant, marginTop: 4}}>{item.year || '—'} · {item.mediaType === 'movie' ? 'Film' : 'Serial'}</AppText>
              </View>
            </View>
          </Pressable>
        )}
        ListEmptyComponent={
          <View style={{padding: 24, alignItems: 'center', gap: 12}}>
            {result.isPending && apiKey ? <ActivityIndicator color={colors.primary} /> : <AppText style={{textAlign: 'center', color: colors.onSurfaceVariant}}>{!apiKey ? 'Atur API key TMDB di Pengaturan Metadata.' : result.isError ? 'Katalog gagal dimuat. Periksa koneksi atau API key TMDB.' : 'Belum ada judul di halaman ini.'}</AppText>}
            {result.isError && <Pressable onPress={() => result.refetch()}><AppText style={{color: colors.primary}}>Coba lagi</AppText></Pressable>}
          </View>
        }
        ListFooterComponent={result.hasNextPage ? (
          <Pressable accessibilityRole="button" disabled={result.isFetchingNextPage} onPress={() => result.fetchNextPage()} style={{alignItems: 'center', margin: 12, padding: 14, borderRadius: 16, backgroundColor: colors.surfaceContainerHigh}}>
            {result.isFetchingNextPage ? <ActivityIndicator color={colors.primary} /> : <AppText style={{color: colors.onSurface}}>{result.isFetchNextPageError ? 'Ulangi muat halaman' : 'Muat lebih banyak'}</AppText>}
          </Pressable>
        ) : null}
      />
    </View>
  );
}
