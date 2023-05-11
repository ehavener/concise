package com.concise.backend;

import com.concise.backend.model.ChapterEntity;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChapterServiceImpl {
    @Autowired
    private ChapterRepository chapterRepository;

    @Transactional
    public ChapterEntity addChapter(ChapterEntity chapter) {
        return chapterRepository.saveAndFlush(chapter);
    }

    public List<ChapterEntity> getChaptersByVideoId(Long videoId) {
        return chapterRepository.findByVideoId(videoId);
    }
}
