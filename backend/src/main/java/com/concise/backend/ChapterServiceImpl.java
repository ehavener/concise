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

    public ChapterEntity getChapter(Integer id) {
        return chapterRepository.findById(id).orElse(null);
    }

    @Transactional
    public ChapterEntity addChapter(ChapterEntity chapter) {
        return chapterRepository.saveAndFlush(chapter);
    }

    @Transactional
    public ChapterEntity updateChapter(ChapterEntity chapter) {
        return chapterRepository.saveAndFlush(chapter);
    }

    public List<ChapterEntity> getChaptersByVideoIdOrderByStartTimeSecondsAsc(Long videoId) {
        return chapterRepository.findByVideoIdOrderByStartTimeSecondsAsc(videoId);
    }
}
