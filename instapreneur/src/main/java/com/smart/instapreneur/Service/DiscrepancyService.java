package com.smart.instapreneur.Service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.smart.instapreneur.Exception.IllegalArgumentException;
import com.smart.instapreneur.Exception.ResourceNotFoundException;
import com.smart.instapreneur.Model.Discrepancy;
import com.smart.instapreneur.Repository.DiscrepancyRepository;

/** Tofauti za stock au malipo zinazohitaji uchunguzi. Si ushahidi wa wizi; ni ukumbusho wa kuangalia. */
@Service
public class DiscrepancyService {
    public static final String STOCK = "STOCK";
    public static final String PAYMENT = "PAYMENT";

    private final DiscrepancyRepository repository;
    private final AuditService audit;

    public DiscrepancyService(DiscrepancyRepository repository, AuditService audit) {
        this.repository = repository;
        this.audit = audit;
    }

    /**
     * Inafungua kesi ndani ya transaction ya kitendo kinachoendelea. Kitendo kikishindwa na kurudishwa nyuma
     * (rollback), kesi pia inarudishwa nyuma.
     */
    @Transactional
    public Discrepancy open(Long storeId, String kind, String reference, String description, String actor) {
        Discrepancy d = new Discrepancy();
        d.setStoreId(storeId);
        d.setKind(kind);
        d.setReference(reference);
        d.setDescription(description.length() > 500 ? description.substring(0, 500) : description);
        d.setStatus("OPEN");
        d.setCreatedBy(actor == null || actor.isBlank() ? "system" : actor);
        d.setCreatedAt(LocalDateTime.now());
        return repository.save(d);
    }

    /**
     * Inafungua kesi kwa transaction YAKE (REQUIRES_NEW): inabaki hata kama kitendo kilichoitisha kikakataliwa
     * kwa kosa. Tunaitumia kwa majaribio yenye mashaka (kiasi kinachozidi deni, namba ya muamala iliyotumika)
     * ambayo yanakataliwa lakini mmiliki anapaswa kuyaona.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Discrepancy openIndependent(Long storeId, String kind, String reference, String description, String actor) {
        return open(storeId, kind, reference, description, actor);
    }

    @Transactional(readOnly = true)
    public List<Discrepancy> list(Long storeId) {
        return repository.findByStoreIdOrderByCreatedAtDesc(storeId);
    }

    @Transactional
    public Discrepancy resolve(Long id, String note, String actor) {
        Discrepancy d = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rekodi ya tofauti haipatikani."));
        if (!"OPEN".equals(d.getStatus())) {
            throw new IllegalArgumentException("Tofauti hii tayari imefungwa.");
        }
        if (note == null || note.isBlank()) {
            throw new IllegalArgumentException("Andika maelezo ya uchunguzi uliofanyika.");
        }
        d.setStatus("RESOLVED");
        d.setResolvedBy(actor);
        d.setResolvedAt(LocalDateTime.now());
        d.setResolutionNote(note.trim().length() > 255 ? note.trim().substring(0, 255) : note.trim());
        Discrepancy saved = repository.save(d);
        audit.record(d.getStoreId(), actor, "DISCREPANCY_RESOLVED", "DISCREPANCY", id, "OPEN", "RESOLVED", saved.getResolutionNote());
        return saved;
    }
}
