package com.concise.backend;


import com.concise.backend.model.UserEntity;
import com.concise.backend.model.VideoDto;
import com.concise.backend.model.VideoEntity;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

//import javax.servlet.http.HttpServlet;
//import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("videos")
public class VideoController {

    @Autowired
    private VideoServiceImpl videoService;

    @GetMapping(path="/{id}", produces = "application/json")
    public @ResponseBody Optional<VideoDto> getVideo(@PathVariable int id) {
        return videoService.getVideoById(id);
    }
}
