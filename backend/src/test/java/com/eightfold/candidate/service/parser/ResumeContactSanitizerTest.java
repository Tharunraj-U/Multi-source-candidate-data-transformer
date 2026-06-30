package com.eightfold.candidate.service.parser;

import com.eightfold.candidate.domain.enums.LinkType;
import com.eightfold.candidate.domain.model.ParsedLinkDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResumeContactSanitizerTest {

    @Test
    void filterEmailsKeepsOnlyValidAddresses() {
        List<String> raw = List.of(
                "linkedin.com/in/tharun-raj-u",
                "tharunraj-u.vercel.app",
                "tharunraj2023@gmail.com",
                "not-an-email");

        List<String> filtered = ResumeContactSanitizer.filterEmails(raw);

        assertEquals(List.of("tharunraj2023@gmail.com"), filtered);
    }

    @Test
    void linksFromMisplacedContactsRecoversUrlsFromEmailList() {
        List<String> raw = List.of(
                "linkedin.com/in/tharun-raj-u",
                "tharunraj-u.vercel.app",
                "tharunraj2023@gmail.com");

        List<ParsedLinkDTO> links = ResumeContactSanitizer.linksFromMisplacedContacts(raw, List.of());

        assertEquals(2, links.size());
        assertTrue(links.stream().anyMatch(l -> l.getType() == LinkType.LINKEDIN
                && l.getUrl().contains("linkedin.com/in/tharun-raj-u")));
        assertTrue(links.stream().anyMatch(l -> l.getType() == LinkType.PORTFOLIO
                && l.getUrl().contains("tharunraj-u.vercel.app")));
    }

    @Test
    void inferLinkTypeClassifiesKnownHosts() {
        assertEquals(LinkType.LINKEDIN, ResumeContactSanitizer.inferLinkType("https://linkedin.com/in/user"));
        assertEquals(LinkType.GITHUB, ResumeContactSanitizer.inferLinkType("https://github.com/user"));
        assertEquals(LinkType.PORTFOLIO, ResumeContactSanitizer.inferLinkType("https://user.vercel.app"));
    }
}
