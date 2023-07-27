import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@Slf4j
public class CrptApi {
    private final TimeUnit timeUnit;
    private final int requestLimit;
    private final RestTemplate restTemplate;
    private final Lock lock;
    private int requestAttempts;
    long timeLimitInMillis;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.requestLimit = requestLimit;
        this.timeUnit = timeUnit;
        restTemplate = new RestTemplate();
        lock = new ReentrantLock();
        timeLimitInMillis = timeUnit.toMillis(1);
    }


    public void createDocument(DocumentDto documentDto, String token) {
        lock.lock();

        long start = System.currentTimeMillis();

        if (requestAttempts == requestLimit) {
            try {
                Thread.sleep(timeLimitInMillis - (System.currentTimeMillis() - start));
                requestAttempts = 0;
                timeLimitInMillis = timeUnit.toMillis(1);
            } catch (InterruptedException e) {
                log.error(e.getMessage());
            }
        } else {
            sendRequest(documentDto, token);
            requestAttempts++;
            timeLimitInMillis -= (System.currentTimeMillis() - start);
        }

        lock.unlock();
    }

    private void sendRequest(DocumentDto document, String token) {
        String url = "/api/v3/lk/documents/commissioning/contract/create";

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);

        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer " + token);

        ConcurrentMap<String, String> map = new ConcurrentHashMap<>();
        map.put("document_format", document.getDocumentFormat());
        map.put("product_document", document.getProductDocument());
        map.put("product_group", document.getProductGroup());
        map.put("signature", document.getSignature());
        map.put("type", document.getType());

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(map, headers);

        ResponseEntity<DocumentDto> response = this.restTemplate.postForEntity(url, entity, DocumentDto.class);

        if (response.getStatusCode() == HttpStatus.CREATED) {
            System.out.println(response.getStatusCode());
        }
    }

    @Data
    public static class DocumentDto {
        private String documentFormat;
        private String productDocument;
        private String productGroup;
        private String signature;
        private String type;
    }

}

