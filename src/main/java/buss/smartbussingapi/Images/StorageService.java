package buss.smartbussingapi.Images;

import lombok.Builder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;



@Service
public class StorageService {

    @Autowired
    private FileDataRepository fileDataRepository;

    private final String FOLDER_PATH = "C:\\Users\\emili\\IdeaProjects\\smart-bussing-back\\src\\main\\java\\buss\\smartbussingapi\\MyFiles\\";

    public StorageService(FileDataRepository finalRepository) {
        this.fileDataRepository = finalRepository;
    }

    public String uploadImageToFileSystem(MultipartFile file) throws IOException {
        String fileName = FOLDER_PATH+file.getOriginalFilename();
        FileData fileData = fileDataRepository.save(FileData.builder()
                .fileName(file.getOriginalFilename())
                .type(file.getContentType())
                .path(fileName).build());

        file.transferTo(new File(fileName));

        if(fileData != null){
            return "File successfully uploaded" + fileName;
        }

        return null;
    }

    public byte[] downlandImageFromFileSystem(String filename) throws IOException {
        Optional<FileData> fileDataOptional = fileDataRepository.findByFileName(filename);
        String filePath = fileDataOptional.get().getPath();
        byte[] images = Files.readAllBytes(new File(filePath).toPath());
        return images;
    }


}
