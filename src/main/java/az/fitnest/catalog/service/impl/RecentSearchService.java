package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.RecentSearchDto;
import az.fitnest.catalog.model.entity.RecentSearch;
import az.fitnest.catalog.repository.RecentSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecentSearchService {

    private final RecentSearchRepository recentSearchRepository;
    private static final int MAX_RECENT_SEARCHES = 10;

    @Transactional
    public void saveSearch(Long userId, String query, String type) {
        if (userId == null || query == null || query.trim().isEmpty() || type == null) {
            return;
        }

        String trimmedQuery = query.trim();

        // Check if query exactly exists (case-insensitive) for this user and type
        Optional<RecentSearch> existingSearch = recentSearchRepository.findByUserIdAndTypeAndQueryIgnoreCase(userId, type, trimmedQuery);
        
        if (existingSearch.isPresent()) {
            RecentSearch search = existingSearch.get();
            search.setCreatedDate(LocalDateTime.now());
            recentSearchRepository.save(search);
        } else {
            RecentSearch newSearch = RecentSearch.builder()
                    .userId(userId)
                    .query(trimmedQuery)
                    .type(type)
                    .build();
            newSearch.setCreatedDate(LocalDateTime.now());
            recentSearchRepository.save(newSearch);
            
            // Limit to 10 latest searches
            long count = recentSearchRepository.countByUserIdAndType(userId, type);
            if (count > MAX_RECENT_SEARCHES) {
                List<RecentSearch> searchesToKeep = recentSearchRepository.findByUserIdAndTypeOrderByCreatedDateDesc(
                        userId, type, PageRequest.of(0, MAX_RECENT_SEARCHES));
                
                if (searchesToKeep.size() == MAX_RECENT_SEARCHES) {
                    RecentSearch oldestToKeep = searchesToKeep.get(searchesToKeep.size() - 1);
                    // Find all searches older than the 10th one to delete
                    List<RecentSearch> allSearches = recentSearchRepository.findByUserIdAndTypeOrderByCreatedDateDesc(
                            userId, type, PageRequest.of(0, Integer.MAX_VALUE));
                    
                    for (int i = MAX_RECENT_SEARCHES; i < allSearches.size(); i++) {
                        recentSearchRepository.delete(allSearches.get(i));
                    }
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public List<RecentSearchDto> getRecentSearches(Long userId, String type) {
        if (userId == null || type == null) {
            return List.of();
        }
        
        return recentSearchRepository.findByUserIdAndTypeOrderByCreatedDateDesc(userId, type, PageRequest.of(0, MAX_RECENT_SEARCHES))
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteSearch(Long userId, String type, String query) {
        if (userId == null || type == null || query == null) return;
        recentSearchRepository.deleteByUserIdAndTypeAndQueryIgnoreCase(userId, type, query.trim());
    }

    @Transactional
    public void clearAllSearches(Long userId, String type) {
        if (userId == null || type == null) return;
        recentSearchRepository.deleteByUserIdAndType(userId, type);
    }

    private RecentSearchDto mapToDto(RecentSearch search) {
        return RecentSearchDto.builder()
                .query(search.getQuery())
                .type(search.getType())
                .createdDate(search.getCreatedDate())
                .build();
    }
}
