package rw.vikingfc.club.partner;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.common.exception.NotFoundException;
import rw.vikingfc.club.common.util.Money;
import rw.vikingfc.club.finance.InstalmentStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class PartnerService {

    private final PartnerRepository partnerRepository;
    private final PartnerPaymentRepository paymentRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list() {
        return partnerRepository.findAll().stream().map(this::view).toList();
    }

    public Map<String, Object> create(PartnerWrite request) {
        Partner partner = new Partner();
        apply(partner, request);
        return view(partnerRepository.save(partner));
    }

    public Map<String, Object> addPayment(Long partnerId, LocalDate dueDate, BigDecimal amount) {
        Partner partner = partnerRepository.findById(partnerId).orElseThrow(() -> NotFoundException.of("Partner", partnerId));
        PartnerPayment payment = new PartnerPayment();
        payment.setPartner(partner);
        payment.setDueDate(dueDate);
        payment.setAmountDue(Money.nz(amount));
        payment.setAmountPaid(Money.zero());
        payment.setStatus(InstalmentStatus.NOT_PAID);
        paymentRepository.save(payment);
        return view(partner);
    }

    public Map<String, Object> recordPayment(Long paymentId, BigDecimal amount, String reference) {
        PartnerPayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> NotFoundException.of("Partner payment", paymentId));
        payment.setAmountPaid(payment.getAmountPaid().add(Money.nz(amount)));
        payment.setReferenceNo(reference);
        if (payment.getAmountPaid().compareTo(payment.getAmountDue()) >= 0) {
            payment.setAmountPaid(payment.getAmountDue());
            payment.setStatus(InstalmentStatus.PAID);
        } else if (payment.getAmountPaid().signum() > 0) {
            payment.setStatus(InstalmentStatus.PARTIALLY_PAID);
        }
        return view(payment.getPartner());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> report() {
        List<Partner> partners = partnerRepository.findAll();
        BigDecimal total = partners.stream().map(Partner::getContractValue).reduce(Money.zero(), BigDecimal::add);
        Partner top = partners.stream().max(Comparator.comparing(Partner::getContractValue)).orElse(null);
        LocalDate today = LocalDate.now(clock);
        List<String> expiring = partners.stream()
                .filter(partner -> partner.getContractEnd() != null && !partner.getContractEnd().isBefore(today)
                        && !partner.getContractEnd().isAfter(today.plusMonths(3)))
                .map(Partner::getName).toList();
        BigDecimal unpaid = paymentRepository.findAll().stream()
                .filter(payment -> payment.getStatus() != InstalmentStatus.PAID)
                .map(payment -> payment.getAmountDue().subtract(payment.getAmountPaid()))
                .reduce(Money.zero(), BigDecimal::add);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("totalContractValue", total);
        body.put("mostValuablePartner", top == null ? null : top.getName());
        body.put("mostValuableAmount", top == null ? Money.zero() : top.getContractValue());
        body.put("expiringWithin3Months", expiring);
        body.put("unpaidInvoices", unpaid);
        return body;
    }

    private void apply(Partner partner, PartnerWrite request) {
        partner.setName(request.name().trim());
        partner.setCategory(request.category());
        partner.setContactName(request.contactName());
        partner.setContactEmail(request.contactEmail());
        partner.setContactPhone(request.contactPhone());
        partner.setContractStart(request.contractStart());
        partner.setContractEnd(request.contractEnd());
        partner.setContractValue(Money.nz(request.contractValue()));
        partner.setBenefits(request.benefits());
        partner.setStatus(request.status() == null ? PartnerStatus.ACTIVE : request.status());
    }

    private Map<String, Object> view(Partner partner) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", partner.getId());
        body.put("name", partner.getName());
        body.put("logoUrl", partner.getLogoUrl());
        body.put("category", partner.getCategory());
        body.put("contactName", partner.getContactName());
        body.put("contractStart", partner.getContractStart());
        body.put("contractEnd", partner.getContractEnd());
        body.put("contractValue", partner.getContractValue());
        body.put("benefits", partner.getBenefits());
        body.put("status", partner.getStatus());
        body.put("payments", paymentRepository.findByPartnerIdOrderByDueDateAsc(partner.getId()).stream().map(payment -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", payment.getId());
            row.put("dueDate", payment.getDueDate());
            row.put("amountDue", payment.getAmountDue());
            row.put("amountPaid", payment.getAmountPaid());
            row.put("status", payment.getStatus());
            return row;
        }).toList());
        return body;
    }

    public record PartnerWrite(@NotBlank String name, @NotNull PartnerCategory category, String contactName,
                               String contactEmail, String contactPhone, LocalDate contractStart, LocalDate contractEnd,
                               BigDecimal contractValue, String benefits, PartnerStatus status) {
    }
}
