package com.eightfold.candidate.controller;

import com.eightfold.candidate.domain.enums.CandidateStatus;
import com.eightfold.candidate.dto.request.ProcessRequest;
import com.eightfold.candidate.dto.response.ApiDtos;
import com.eightfold.candidate.exception.CandidateNotFoundException;
import com.eightfold.candidate.service.CandidateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/candidate")
@RequiredArgsConstructor
public class CandidateController {

    private final CandidateService candidateService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiDtos.UploadResponseDto> upload(
            @RequestParam(value = "resume", required = false) MultipartFile resume,
            @RequestParam(value = "recruiterCsv", required = false) MultipartFile recruiterCsv,
            @RequestParam(value = "atsJson", required = false) MultipartFile atsJson,
            @RequestParam(value = "gitHubUrl", required = false) String gitHubUrl,
            @RequestParam(value = "runtimeConfig", required = false) String runtimeConfig) throws IOException {

        ApiDtos.UploadResponseDto response = candidateService.upload(
                resume, recruiterCsv, atsJson, gitHubUrl, runtimeConfig);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/process")
    public ResponseEntity<ApiDtos.ProcessResponseDto> process(@Valid @RequestBody ProcessRequest request) {
        ApiDtos.ProcessResponseDto response = candidateService.startProcessing(request.getCandidateId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{id}")
    public Object getCandidate(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean projection,
            @RequestParam(defaultValue = "true") boolean includeProvenance,
            @RequestParam(defaultValue = "true") boolean includeConfidence) {
        if (projection) {
            return candidateService.getProjectedCandidate(id);
        }
        return candidateService.getCandidate(id, includeProvenance, includeConfidence);
    }

    @GetMapping
    public ApiDtos.CandidateListResponseDto listCandidates(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) BigDecimal minConfidence,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) CandidateStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        return candidateService.listCandidates(search, minConfidence, company, status, page, size, sort);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCandidate(@PathVariable UUID id) {
        candidateService.deleteCandidate(id);
    }

    @PostMapping("/{id}/reprocess")
    public ResponseEntity<ApiDtos.ProcessResponseDto> reprocess(@PathVariable UUID id) {
        ApiDtos.ProcessResponseDto response = candidateService.startReprocessing(id);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{id}/job/{jobId}")
    public ApiDtos.JobStatusResponseDto getJobStatus(@PathVariable UUID id, @PathVariable UUID jobId) {
        return candidateService.getJobStatus(id, jobId);
    }

    @GetMapping("/{id}/resume")
    public ResponseEntity<Resource> downloadResume(@PathVariable UUID id) throws IOException {
        InputStreamResource resource = new InputStreamResource(candidateService.getResumeStream(id));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"resume\"")
                .body(resource);
    }

    @GetMapping("/{id}/picture")
    public ResponseEntity<Resource> downloadPicture(@PathVariable UUID id) throws IOException {
        try {
            InputStreamResource resource = new InputStreamResource(candidateService.getPictureStream(id));
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(resource);
        } catch (CandidateNotFoundException ex) {
            return ResponseEntity.notFound().build();
        }
    }
}
