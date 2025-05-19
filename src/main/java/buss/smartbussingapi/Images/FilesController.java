package buss.smartbussingapi.Images;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping(path = "api/v1/images")
public class FilesController {

    @Autowired
    private final StorageService storageService;

    public FilesController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping(path = "/fileSystem")
    public ResponseEntity<?> uploadImageToFileSystem(@RequestParam("image") MultipartFile file) throws IOException {
        String uploadImage = storageService.uploadImageToFileSystem(file);
        return ResponseEntity.ok().body(uploadImage);
    }

    @GetMapping(path = "/{imageFile}")
    public ResponseEntity<?> downloadImageFromFileSystem(@PathVariable("imageFile") String fileName) throws IOException {
        byte[] image = storageService.downlandImageFromFileSystem(fileName);
        return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.valueOf("image/png")).body(image);
    }

}
