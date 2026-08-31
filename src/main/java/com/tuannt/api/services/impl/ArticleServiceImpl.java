package com.tuannt.api.services.impl;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.tuannt.api.configs.ArticleConfig;
import com.tuannt.api.dtos.article.ArticleDetailDto;
import com.tuannt.api.dtos.article.ArticleReqDto;
import com.tuannt.api.dtos.article.ArticleResDto;
import com.tuannt.api.exceptions.BadRequestException;
import com.tuannt.api.services.ArticleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by tuannt7 on 14/04/2025
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {
    private final ArticleConfig articleConfig;

    private static String extractImageUrl(String html) {
        Pattern pattern = Pattern.compile("<img[^>]+src=[\"']([^\"']+)[\"']");
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    @Override
    public Map<String, List<String>> getSources() {
        return articleConfig.getSources();
    }

    @Override
    public ArticleResDto getArticle(ArticleReqDto articleReqDto) {
        Optional<ArticleConfig.ArticleRSS> articleRSS = articleConfig.getArticle(articleReqDto.getSource(), articleReqDto.getCategory());
        if (articleRSS.isEmpty()) {
            throw new BadRequestException("Invalid source or category");
        }

        ArticleConfig.ArticleRSS article = articleRSS.get();
        ArticleResDto articleResDto = new ArticleResDto();
        List<ArticleDetailDto> feedsList = new ArrayList<>();
        try {
            URL feedSource = new URL(article.getUrl());
            SyndFeed feed = new SyndFeedInput().build(new XmlReader(feedSource));

            for (SyndEntry entry : feed.getEntries()) {
                String descriptionHtml = entry.getDescription() != null ? entry.getDescription().getValue() : "";
                String imageUrl = extractImageUrl(descriptionHtml);
                String textDescription = descriptionHtml.replaceAll("<[^>]*>", "").trim();

                ArticleDetailDto item = new ArticleDetailDto();
                item.setTitle(entry.getTitle());
                item.setLink(entry.getLink());
                item.setPublishedDate(entry.getPublishedDate());
                item.setImage(imageUrl);
                item.setDescription(textDescription);

                feedsList.add(item);
            }

            articleResDto.setTitle(feed.getTitle());
            articleResDto.setUpdate(feed.getPublishedDate());
            articleResDto.setSize(feedsList.size());
            articleResDto.setFeeds(feedsList);
            articleResDto.setSource(articleReqDto.getSource());
            articleResDto.setCategory(articleReqDto.getCategory());

        } catch (Exception ex) {
            log.error("Error fetching articles input: {} ex: ", ex.getMessage(), ex);
        }
        return articleResDto;
    }
}
