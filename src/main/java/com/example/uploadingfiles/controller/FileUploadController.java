package com.example.uploadingfiles.controller;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.uploadingfiles.storage.StorageFileNotFoundException;
import com.example.uploadingfiles.storage.StorageService;

/**
 * Controllers for a sample web app that deal with file upload and serving
 */
@Controller
public class FileUploadController {

    /**
     * Injecting the StorageService
     */
    private final StorageService storageService;

    @Autowired
    public FileUploadController(StorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * List all files in the storage providing a link for download and one for display
     * Display a file upload form
     */
    @GetMapping("/")
    public String listUploadedFiles(Model model) throws IOException {

        model.addAttribute("files", storageService.loadAll().map(
                path -> MvcUriComponentsBuilder.fromMethodName(FileUploadController.class,
                        "serveFile", path.getFileName().toString()).build().toString())
                .collect(Collectors.toList()));
        model.addAttribute("shortFiles", storageService.loadAll().collect(Collectors.toList()));
        return "uploadForm";
    }

    /**
     * Serving a file for download
     *
     * @param filename
     * @return the ResponseEntity
     */
    @GetMapping("/files/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        Resource file = storageService.loadAsResource(filename);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + file.getFilename() + "\"").body(file);
    }

    /**
     * Serving a file
     *
     * @param filename
     * @return the ResponseEntity
     */
    @GetMapping("/get/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> displayFile(@PathVariable String filename) throws IOException {
        Resource file = storageService.loadAsResource(filename);
        Optional<MediaType> type = MediaTypeFactory.getMediaType(file);
        if (type.isPresent()) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "filename=\"" + file.getFilename() + "\"")
                    .contentType(type.get())
                    .contentLength(file.contentLength())
                    .body(file);
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Type of the file could be determined");
    }

    /**
     * Get the file send from a form
     *
     * @param file               from the form
     * @param redirectAttributes the flash message to be displayed in a later controller
     * @return a redirect String
     */
    @PostMapping("/")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        storageService.store(file);
        redirectAttributes.addFlashAttribute("message", "You successfully uploaded " + file.getOriginalFilename() + "!");
        return "redirect:/";
    }

    /**
     * Indicate how Spring is supposed to deal with Exceptions thrown by controllers
     *
     * @param exc StorageFileNotFoundException
     * @return a HTTP NOT FOUND response
     */
    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }

}