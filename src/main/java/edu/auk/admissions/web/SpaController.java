package edu.auk.admissions.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * The front end is a React single-page app (frontend/). Its pages are routes inside the app, so when a
 * browser opens one of them directly, the server answers with the app's index.html and React shows the page.
 */
@Controller
public class SpaController {

    /** Pages of the React app, see frontend/src/App.jsx. */
    public static final String[] ROUTES = {"/", "/login", "/signup", "/applicant", "/staff"};

    @GetMapping({"/", "/login", "/signup", "/applicant", "/staff"}) // same as ROUTES
    public String app() {
        return "forward:/index.html";
    }
}
