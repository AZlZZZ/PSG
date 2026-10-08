package com.psg.config;

import com.psg.model.*;
import com.psg.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SectorRepository sectorRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private SecurityActionRepository securityActionRepository;

    @Autowired
    private ActionSectorCoefficientRepository coefficientRepository;

    // Sector order exactly as defined in the matrix column order:
    // Santé | Finance | Énergie | Éducation | IT / Télécom | Agriculture | Industrie | Transport | Commerce
    public static final String[] SECTORS = {
        "Santé",
        "Finance",
        "Énergie",
        "Éducation",
        "IT / Télécom",
        "Agriculture",
        "Industrie",
        "Transport",
        "Commerce"
    };

    private static class ActionDef {
        final String code;
        final String isoControl;
        final String theme;
        final String projectSubdomain;
        final String description;
        final int[] coefficients; // 9 values corresponding to SECTORS

        ActionDef(String code, String isoControl, String theme, String projectSubdomain, String description, int[] coefficients) {
            this.code = code;
            this.isoControl = isoControl;
            this.theme = theme;
            this.projectSubdomain = projectSubdomain;
            this.description = description;
            this.coefficients = coefficients;
        }
    }

    private static final List<ActionDef> ACTIONS = Arrays.asList(
        // ORGANIZATIONAL — A01–A37
        new ActionDef("A01", "ISO 5.1", "Organizational", "Governance & Security Policies",
            "L'organisme dispose d'une politique de sécurité de l'information formalisée, approuvée par la direction, communiquée aux parties concernées et régulièrement revue.",
            new int[]{5, 5, 5, 4, 5, 3, 5, 5, 4}),
        new ActionDef("A02", "ISO 5.2", "Organizational", "Governance & Security Policies",
            "L'organisme définit et attribue clairement les rôles et responsabilités liés à la sécurité de l'information aux personnes concernées.",
            new int[]{5, 5, 5, 4, 5, 3, 5, 5, 4}),
        new ActionDef("A03", "ISO 5.3", "Organizational", "Governance & Security Policies",
            "L'organisme sépare les responsabilités incompatibles afin de réduire les risques d'erreur, de fraude ou d'utilisation abusive des ressources et informations.",
            new int[]{4, 5, 5, 3, 5, 3, 5, 4, 5}),
        new ActionDef("A04", "ISO 5.4", "Organizational", "Governance & Security Policies",
            "La direction définit, communique et assume ses responsabilités en matière de sécurité de l'information.",
            new int[]{5, 5, 5, 4, 5, 3, 5, 5, 4}),
        new ActionDef("A05", "ISO 5.8", "Organizational", "Governance & Security Policies",
            "Les exigences de sécurité de l'information sont prises en compte dès la conception et tout au long du cycle de vie des projets.",
            new int[]{5, 5, 5, 4, 5, 3, 5, 5, 5}),

        new ActionDef("A06", "ISO 5.5", "Organizational", "Risk Management",
            "L'organisme définit les procédures et responsabilités nécessaires pour communiquer avec les autorités compétentes en cas de besoin lié à la sécurité de l'information.",
            new int[]{4, 5, 5, 3, 4, 3, 5, 5, 4}),
        new ActionDef("A07", "ISO 5.6", "Organizational", "Risk Management",
            "L'organisme maintient des relations avec des groupes, associations ou communautés spécialisés afin de suivre les bonnes pratiques et l'évolution des menaces de sécurité.",
            new int[]{3, 4, 4, 3, 5, 2, 4, 3, 3}),
        new ActionDef("A08", "ISO 5.7", "Organizational", "Risk Management",
            "L'organisme met en place un processus de collecte, analyse et utilisation des informations sur les menaces afin d'identifier les risques pertinents pour ses activités.",
            new int[]{5, 5, 5, 3, 5, 3, 5, 5, 4}),

        new ActionDef("A09", "ISO 5.9", "Organizational", "Asset & Information Management",
            "L'organisme maintient un inventaire à jour des informations et des actifs associés, avec leurs propriétaires/responsables identifiés.",
            new int[]{5, 5, 5, 4, 5, 4, 5, 5, 5}),
        new ActionDef("A10", "ISO 5.10", "Organizational", "Asset & Information Management",
            "L'organisme définit des règles d'utilisation acceptable des informations et des actifs, communiquées aux utilisateurs concernés.",
            new int[]{4, 5, 4, 4, 5, 3, 4, 4, 5}),
        new ActionDef("A11", "ISO 5.11", "Organizational", "Asset & Information Management",
            "Les actifs appartenant à l'organisme sont restitués lors de la fin ou du changement des responsabilités d'un utilisateur.",
            new int[]{4, 4, 4, 4, 4, 3, 4, 4, 4}),
        new ActionDef("A12", "ISO 5.12", "Organizational", "Asset & Information Management",
            "L'organisme classe les informations selon leur niveau de sensibilité, de valeur et les besoins de protection.",
            new int[]{5, 5, 5, 4, 5, 3, 5, 5, 5}),
        new ActionDef("A13", "ISO 5.13", "Organizational", "Asset & Information Management",
            "Les informations classifiées sont étiquetées conformément à leur niveau de classification lorsque nécessaire.",
            new int[]{4, 4, 4, 3, 5, 3, 4, 4, 4}),
        new ActionDef("A14", "ISO 5.14", "Organizational", "Asset & Information Management",
            "L'organisme définit et applique des règles sécurisées pour le transfert des informations, notamment avec les parties externes.",
            new int[]{5, 5, 5, 4, 5, 3, 5, 5, 5}),

        new ActionDef("A15", "ISO 5.15", "Organizational", "Access & Identity Management",
            "L'organisme définit et applique une politique de contrôle d'accès basée sur les besoins métier et les exigences de sécurité.",
            new int[]{5, 5, 5, 4, 5, 3, 5, 5, 5}),
        new ActionDef("A16", "ISO 5.16", "Organizational", "Access & Identity Management",
            "Chaque utilisateur dispose d'une identité unique, permettant d'identifier et de gérer les utilisateurs de manière fiable.",
            new int[]{5, 5, 5, 4, 5, 3, 5, 5, 5}),
        new ActionDef("A17", "ISO 5.17", "Organizational", "Access & Identity Management",
            "Les informations d'authentification sont protégées, gérées et communiquées aux utilisateurs de manière sécurisée.",
            new int[]{5, 5, 5, 4, 5, 3, 5, 5, 5}),
        new ActionDef("A18", "ISO 5.18", "Organizational", "Access & Identity Management",
            "Les droits d'accès sont accordés, modifiés et retirés selon des procédures définies, avec une révision périodique des accès.",
            new int[]{5, 5, 5, 4, 5, 3, 5, 5, 5}),

        new ActionDef("A19", "ISO 5.19", "Organizational", "Supplier & Third-Party Security",
            "L'organisme identifie et évalue les risques de sécurité liés à ses fournisseurs avant et pendant la relation.",
            new int[]{4, 5, 5, 3, 5, 4, 5, 5, 5}),
        new ActionDef("A20", "ISO 5.20", "Organizational", "Supplier & Third-Party Security",
            "Les exigences de sécurité de l'information sont définies et intégrées dans les accords avec les fournisseurs.",
            new int[]{4, 5, 5, 3, 5, 4, 5, 5, 5}),
        new ActionDef("A21", "ISO 5.21", "Organizational", "Supplier & Third-Party Security",
            "L'organisme évalue et maîtrise les risques de sécurité liés à sa chaîne d'approvisionnement ICT.",
            new int[]{4, 5, 5, 3, 5, 4, 5, 5, 5}),
        new ActionDef("A22", "ISO 5.22", "Organizational", "Supplier & Third-Party Security",
            "Les services des fournisseurs font l'objet d'une surveillance, d'une revue et d'une gestion des changements liés à la sécurité.",
            new int[]{4, 5, 5, 3, 5, 4, 5, 5, 5}),
        new ActionDef("A23", "ISO 5.23", "Organizational", "Supplier & Third-Party Security",
            "L'organisme définit et applique des mesures de sécurité adaptées à l'utilisation des services cloud.",
            new int[]{5, 5, 5, 5, 5, 3, 4, 4, 5}),

        new ActionDef("A24", "ISO 5.24", "Organizational", "Incident Management",
            "L'organisme dispose d'un processus de préparation et de planification de la gestion des incidents de sécurité.",
            new int[]{5, 5, 5, 4, 5, 3, 5, 5, 5}),
        new ActionDef("A25", "ISO 5.25", "Organizational", "Incident Management",
            "Les événements de sécurité sont évalués afin de déterminer s'ils constituent des incidents et leur niveau de gravité.",
            new int[]{5, 5, 5, 4, 5, 3, 5, 5, 5}),
        new ActionDef("A26", "ISO 5.26", "Organizational", "Incident Management",
            "L'organisme dispose de procédures de réponse aux incidents permettant de contenir, traiter et résoudre les incidents de sécurité.",
            new int[]{5, 5, 5, 4, 5, 3, 5, 5, 5}),
        new ActionDef("A27", "ISO 5.27", "Organizational", "Incident Management",
            "Après un incident, l'organisme analyse les causes et tire des enseignements afin d'améliorer sa sécurité.",
            new int[]{4, 5, 5, 3, 5, 3, 5, 5, 4}),
        new ActionDef("A28", "ISO 5.28", "Organizational", "Incident Management",
            "L'organisme dispose de procédures permettant la collecte, la conservation et la protection des preuves liées aux incidents.",
            new int[]{5, 5, 5, 3, 5, 2, 5, 5, 5}),

        new ActionDef("A29", "ISO 5.29", "Organizational", "Business Continuity & Resilience",
            "L'organisme définit des mesures permettant de maintenir la sécurité de l'information pendant une perturbation ou une crise.",
            new int[]{5, 5, 5, 4, 5, 4, 5, 5, 5}),
        new ActionDef("A30", "ISO 5.30", "Organizational", "Business Continuity & Resilience",
            "L'organisme assure la préparation et la disponibilité des systèmes ICT nécessaires à la continuité d'activité.",
            new int[]{5, 5, 5, 4, 5, 4, 5, 5, 5}),

        new ActionDef("A31", "ISO 5.31", "Organizational", "Legal, Regulatory & Compliance",
            "L'organisme identifie et respecte les exigences légales, réglementaires et contractuelles applicables à la sécurité de l'information.",
            new int[]{5, 5, 5, 4, 5, 3, 5, 5, 5}),
        new ActionDef("A32", "ISO 5.32", "Organizational", "Legal, Regulatory & Compliance",
            "L'organisme protège et respecte les droits de propriété intellectuelle liés aux informations et logiciels utilisés.",
            new int[]{4, 5, 4, 4, 5, 3, 5, 4, 5}),
        new ActionDef("A33", "ISO 5.33", "Organizational", "Legal, Regulatory & Compliance",
            "Les enregistrements et documents importants sont protégés contre la perte, la destruction, la falsification et l'accès non autorisé.",
            new int[]{5, 5, 5, 4, 5, 3, 5, 5, 5}),
        new ActionDef("A34", "ISO 5.34", "Organizational", "Legal, Regulatory & Compliance",
            "L'organisme met en œuvre des mesures pour assurer la protection des données personnelles (PII) conformément aux exigences applicables.",
            new int[]{5, 5, 4, 5, 5, 3, 4, 5, 5}),

        new ActionDef("A35", "ISO 5.35", "Organizational", "Security Review & Audit",
            "La sécurité de l'information fait l'objet de revues indépendantes à intervalles planifiés ou lorsque des changements importants surviennent.",
            new int[]{5, 5, 5, 3, 5, 3, 5, 5, 4}),
        new ActionDef("A36", "ISO 5.36", "Organizational", "Security Review & Audit",
            "L'organisme vérifie régulièrement la conformité aux politiques, règles et normes de sécurité établies.",
            new int[]{5, 5, 5, 4, 5, 3, 5, 5, 5}),
        new ActionDef("A37", "ISO 5.37", "Organizational", "Security Review & Audit",
            "L'organisme maintient des procédures opérationnelles documentées pour les activités liées à la sécurité de l'information.",
            new int[]{5, 5, 5, 4, 5, 3, 5, 5, 5}),

        // PEOPLE — A38–A45
        new ActionDef("A38", "ISO 6.1", "People", "Recruitment & Employment",
            "L'organisme effectue des vérifications appropriées des candidats avant leur recrutement, selon les exigences légales et les risques associés au poste.",
            new int[]{4, 5, 5, 4, 5, 3, 4, 4, 4}),
        new ActionDef("A39", "ISO 6.2", "People", "Recruitment & Employment",
            "Les conditions et responsabilités en matière de sécurité de l'information sont clairement définies dans les conditions d'emploi.",
            new int[]{4, 5, 5, 4, 5, 3, 4, 4, 4}),

        new ActionDef("A40", "ISO 6.3", "People", "Awareness & Training",
            "L'organisme fournit aux utilisateurs une sensibilisation, une formation et une éducation régulières en matière de sécurité de l'information.",
            new int[]{5, 5, 5, 5, 5, 4, 5, 5, 5}),

        new ActionDef("A41", "ISO 6.4", "People", "Disciplinary & Confidentiality",
            "L'organisme dispose d'un processus disciplinaire en cas de violation des règles de sécurité de l'information.",
            new int[]{4, 4, 4, 3, 4, 3, 4, 4, 4}),
        new ActionDef("A42", "ISO 6.5", "People", "Disciplinary & Confidentiality",
            "Les responsabilités et obligations de sécurité sont maintenues après la fin ou le changement d'emploi, selon les exigences applicables.",
            new int[]{5, 5, 5, 4, 5, 3, 5, 5, 5}),
        new ActionDef("A43", "ISO 6.6", "People", "Disciplinary & Confidentiality",
            "Les personnes concernées sont soumises à des accords de confidentialité ou de non-divulgation (NDA) lorsque nécessaire.",
            new int[]{5, 5, 5, 4, 5, 3, 5, 5, 5}),

        new ActionDef("A44", "ISO 6.7", "People", "Remote Work & Incident Reporting",
            "L'organisme applique des mesures de sécurité adaptées au travail à distance.",
            new int[]{5, 5, 4, 5, 5, 3, 4, 4, 5}),
        new ActionDef("A45", "ISO 6.8", "People", "Remote Work & Incident Reporting",
            "L'organisme fournit un mécanisme permettant de signaler rapidement les événements et incidents de sécurité.",
            new int[]{5, 5, 5, 4, 5, 3, 5, 5, 5}),

        // PHYSICAL — A46–A59
        new ActionDef("A46", "ISO 7.1", "Physical", "Physical Perimeters & Access",
            "L'organisme définit et sécurise des périmètres physiques pour protéger les zones contenant des informations ou équipements sensibles.",
            new int[]{5, 5, 5, 4, 5, 4, 5, 5, 4}),
        new ActionDef("A47", "ISO 7.2", "Physical", "Physical Perimeters & Access",
            "L'accès aux locaux et zones sensibles est contrôlé et limité aux personnes autorisées.",
            new int[]{5, 5, 5, 4, 5, 3, 5, 5, 4}),
        new ActionDef("A48", "ISO 7.3", "Physical", "Physical Perimeters & Access",
            "Les bureaux, salles et installations sont sécurisés contre les accès ou activités non autorisés.",
            new int[]{5, 5, 5, 4, 5, 4, 5, 5, 4}),

        new ActionDef("A49", "ISO 7.4", "Physical", "Environmental Protection",
            "L'organisme utilise des mesures de surveillance physique adaptées aux risques, telles que la vidéosurveillance lorsque nécessaire.",
            new int[]{5, 5, 5, 3, 5, 3, 5, 5, 5}),
        new ActionDef("A50", "ISO 7.5", "Physical", "Environmental Protection",
            "L'organisme protège ses installations et équipements contre les menaces physiques et environnementales telles que l'incendie, l'inondation ou les conditions extrêmes.",
            new int[]{5, 5, 5, 4, 5, 5, 5, 5, 4}),

        new ActionDef("A51", "ISO 7.6", "Physical", "Secure Areas & Equipment",
            "L'accès et le travail dans les zones sécurisées sont soumis à des règles et contrôles appropriés.",
            new int[]{5, 5, 5, 3, 5, 3, 5, 5, 4}),
        new ActionDef("A52", "ISO 7.7", "Physical", "Secure Areas & Equipment",
            "L'organisme applique une politique de bureau propre et écran propre afin de limiter l'exposition des informations sensibles.",
            new int[]{5, 5, 4, 4, 5, 3, 4, 4, 5}),
        new ActionDef("A53", "ISO 7.8", "Physical", "Secure Areas & Equipment",
            "Les équipements sont positionnés et protégés afin de réduire les risques de dommages, d'accès non autorisé ou d'interception.",
            new int[]{5, 5, 5, 4, 5, 4, 5, 5, 5}),
        new ActionDef("A54", "ISO 7.9", "Physical", "Secure Areas & Equipment",
            "Les équipements et actifs utilisés hors des locaux de l'organisme sont protégés contre la perte, le vol et les accès non autorisés.",
            new int[]{5, 5, 5, 4, 5, 4, 5, 5, 5}),

        new ActionDef("A55", "ISO 7.10", "Physical", "Media & Disposal",
            "Les supports de stockage contenant des informations sont gérés et protégés contre la perte, le vol, l'accès ou la divulgation non autorisés.",
            new int[]{5, 5, 5, 4, 5, 3, 5, 5, 5}),
        new ActionDef("A56", "ISO 7.11", "Physical", "Media & Disposal",
            "Les équipements disposent de services essentiels fiables (électricité, climatisation, réseau, etc.) adaptés à leur criticité.",
            new int[]{5, 5, 5, 4, 5, 5, 5, 5, 4}),
        new ActionDef("A57", "ISO 7.12", "Physical", "Media & Disposal",
            "Les câbles électriques et de communication sont protégés contre les dommages, interceptions ou perturbations.",
            new int[]{4, 5, 5, 3, 5, 4, 5, 5, 4}),
        new ActionDef("A58", "ISO 7.13", "Physical", "Media & Disposal",
            "Les équipements font l'objet d'une maintenance régulière et sécurisée afin de préserver leur disponibilité et leur sécurité.",
            new int[]{5, 5, 5, 4, 5, 4, 5, 5, 4}),
        new ActionDef("A59", "ISO 7.14", "Physical", "Media & Disposal",
            "Les équipements et supports contenant des informations sont effacés ou détruits de manière sécurisée avant leur mise au rebut ou leur réutilisation.",
            new int[]{5, 5, 5, 4, 5, 3, 5, 5, 5}),

        // TECHNOLOGICAL — A60–A93
        new ActionDef("A60", "ISO 8.1", "Technological", "Endpoint & Access Security",
            "L'organisme protège les informations stockées, traitées ou accessibles via les terminaux utilisateurs, au moyen de mesures de sécurité adaptées.",
            new int[]{5, 5, 5, 5, 5, 4, 5, 5, 5}),
        new ActionDef("A61", "ISO 8.2", "Technological", "Endpoint & Access Security",
            "L'organisme restreint, attribue et surveille les droits d'accès privilégiés selon les responsabilités et les besoins.",
            new int[]{5, 5, 5, 4, 5, 3, 5, 5, 5}),
        new ActionDef("A62", "ISO 8.3", "Technological", "Endpoint & Access Security",
            "L'organisme limite l'accès aux informations et aux ressources associées selon les besoins métier et les règles de contrôle d'accès.",
            new int[]{5, 5, 5, 4, 5, 3, 5, 5, 5}),
        new ActionDef("A63", "ISO 8.4", "Technological", "Endpoint & Access Security",
            "L'accès au code source, aux outils et aux bibliothèques de développement est limité aux personnes autorisées et protégé contre les modifications non autorisées.",
            new int[]{4, 5, 4, 3, 5, 2, 5, 4, 4}),
        new ActionDef("A64", "ISO 8.5", "Technological", "Endpoint & Access Security",
            "L'organisme met en œuvre des mécanismes d'authentification sécurisée adaptés aux systèmes, aux utilisateurs et aux risques.",
            new int[]{5, 5, 5, 5, 5, 3, 5, 5, 5}),
        new ActionDef("A65", "ISO 8.6", "Technological", "Endpoint & Access Security",
            "L'organisme surveille et gère la capacité des ressources informatiques afin de maintenir les performances et la disponibilité nécessaires.",
            new int[]{5, 5, 5, 4, 5, 4, 5, 5, 5}),

        new ActionDef("A66", "ISO 8.7", "Technological", "Malware & Vulnerability Management",
            "L'organisme met en œuvre des mesures appropriées de prévention, détection et traitement des logiciels malveillants.",
            new int[]{5, 5, 5, 5, 5, 4, 5, 5, 5}),
        new ActionDef("A67", "ISO 8.8", "Technological", "Malware & Vulnerability Management",
            "L'organisme identifie, évalue, priorise et traite régulièrement les vulnérabilités techniques de ses systèmes.",
            new int[]{5, 5, 5, 4, 5, 4, 5, 5, 5}),

        new ActionDef("A68", "ISO 8.9", "Technological", "Configuration & Data Protection",
            "L'organisme définit et maintient des configurations sécurisées pour ses systèmes, équipements et composants technologiques.",
            new int[]{5, 5, 5, 4, 5, 4, 5, 5, 5}),
        new ActionDef("A69", "ISO 8.10", "Technological", "Configuration & Data Protection",
            "L'organisme supprime les informations lorsqu'elles ne sont plus nécessaires, selon des règles et méthodes adaptées aux risques et aux exigences applicables.",
            new int[]{5, 5, 4, 4, 5, 3, 4, 5, 5}),
        new ActionDef("A70", "ISO 8.11", "Technological", "Configuration & Data Protection",
            "L'organisme applique des techniques de masquage des données lorsque nécessaire afin de réduire l'exposition des informations sensibles.",
            new int[]{5, 5, 4, 4, 5, 2, 4, 4, 5}),
        new ActionDef("A71", "ISO 8.12", "Technological", "Configuration & Data Protection",
            "L'organisme met en œuvre des mesures de prévention des fuites d'informations afin de protéger les informations sensibles contre les divulgations non autorisées.",
            new int[]{5, 5, 5, 4, 5, 3, 5, 5, 5}),
        new ActionDef("A72", "ISO 8.13", "Technological", "Configuration & Data Protection",
            "L'organisme réalise et protège des sauvegardes des informations, logiciels et systèmes selon leur criticité, et vérifie régulièrement leur capacité de restauration.",
            new int[]{5, 5, 5, 5, 5, 5, 5, 5, 5}),
        new ActionDef("A73", "ISO 8.14", "Technological", "Configuration & Data Protection",
            "L'organisme met en œuvre une redondance des moyens de traitement de l'information lorsque nécessaire pour assurer la disponibilité des services.",
            new int[]{5, 5, 5, 4, 5, 4, 5, 5, 5}),

        new ActionDef("A74", "ISO 8.15", "Technological", "Logging & Monitoring",
            "L'organisme génère, protège et conserve des journaux d'événements pertinents afin de permettre la surveillance, l'analyse et les investigations de sécurité.",
            new int[]{5, 5, 5, 4, 5, 3, 5, 5, 5}),
        new ActionDef("A75", "ISO 8.16", "Technological", "Logging & Monitoring",
            "L'organisme met en œuvre une surveillance des activités afin de détecter les comportements, événements ou anomalies susceptibles d'indiquer un problème de sécurité.",
            new int[]{5, 5, 5, 4, 5, 3, 5, 5, 5}),
        new ActionDef("A76", "ISO 8.17", "Technological", "Logging & Monitoring",
            "Les systèmes concernés utilisent une synchronisation fiable de l'heure afin de garantir la cohérence des événements et des journaux.",
            new int[]{4, 5, 5, 3, 5, 3, 5, 5, 4}),
        new ActionDef("A77", "ISO 8.18", "Technological", "Logging & Monitoring",
            "L'utilisation des programmes utilitaires privilégiés est limitée, contrôlée et surveillée afin de prévenir leur utilisation abusive.",
            new int[]{5, 5, 5, 4, 5, 3, 5, 5, 5}),
        new ActionDef("A78", "ISO 8.19", "Technological", "Logging & Monitoring",
            "L'organisme contrôle l'installation de logiciels sur les systèmes opérationnels afin d'empêcher l'installation non autorisée ou présentant un risque de sécurité.",
            new int[]{5, 5, 5, 4, 5, 3, 5, 5, 5}),

        new ActionDef("A79", "ISO 8.20", "Technological", "Network Security",
            "L'organisme protège ses réseaux contre les accès, communications ou activités non autorisés au moyen de mesures de sécurité adaptées.",
            new int[]{5, 5, 5, 4, 5, 4, 5, 5, 5}),
        new ActionDef("A80", "ISO 8.21", "Technological", "Network Security",
            "Les services réseau utilisés par l'organisme sont sécurisés et font l'objet d'exigences et de contrôles de sécurité appropriés.",
            new int[]{5, 5, 5, 4, 5, 4, 5, 5, 5}),
        new ActionDef("A81", "ISO 8.22", "Technological", "Network Security",
            "L'organisme met en œuvre une segmentation des réseaux lorsque nécessaire afin de limiter les communications et la propagation des menaces.",
            new int[]{5, 5, 5, 4, 5, 3, 5, 5, 5}),
        new ActionDef("A82", "ISO 8.23", "Technological", "Network Security",
            "L'organisme applique un filtrage Web adapté afin de réduire l'accès à des contenus ou ressources Web présentant des risques de sécurité.",
            new int[]{4, 5, 4, 5, 5, 3, 4, 4, 5}),

        new ActionDef("A83", "ISO 8.24", "Technological", "Cryptography",
            "L'organisme utilise des mesures cryptographiques adaptées pour protéger les informations, notamment lors de leur stockage ou de leur transmission, et gère de manière sécurisée les éléments cryptographiques associés.",
            new int[]{5, 5, 5, 4, 5, 3, 5, 5, 5}),

        new ActionDef("A84", "ISO 8.25", "Technological", "Secure Development",
            "L'organisme intègre la sécurité dans le cycle de développement des systèmes et applications, de la conception jusqu'à la maintenance.",
            new int[]{4, 5, 5, 4, 5, 3, 5, 4, 5}),
        new ActionDef("A85", "ISO 8.26", "Technological", "Secure Development",
            "Les exigences de sécurité des applications sont identifiées, définies et intégrées lors de leur conception et de leur acquisition.",
            new int[]{5, 5, 5, 4, 5, 3, 5, 5, 5}),
        new ActionDef("A86", "ISO 8.27", "Technological", "Secure Development",
            "L'organisme applique des principes d'architecture et d'ingénierie sécurisées lors de la conception et de l'évolution des systèmes.",
            new int[]{5, 5, 5, 4, 5, 3, 5, 5, 5}),
        new ActionDef("A87", "ISO 8.28", "Technological", "Secure Development",
            "L'organisme applique des pratiques de codage sécurisé afin de réduire les vulnérabilités dans les logiciels développés.",
            new int[]{4, 5, 5, 4, 5, 2, 5, 4, 5}),
        new ActionDef("A88", "ISO 8.29", "Technological", "Secure Development",
            "Les systèmes et applications font l'objet de tests de sécurité pendant le développement et avant leur acceptation.",
            new int[]{5, 5, 5, 4, 5, 3, 5, 5, 5}),
        new ActionDef("A89", "ISO 8.30", "Technological", "Secure Development",
            "Les activités de développement externalisées sont encadrées et soumises à des exigences et contrôles de sécurité appropriés.",
            new int[]{4, 5, 5, 3, 5, 3, 5, 5, 5}),

        new ActionDef("A90", "ISO 8.31", "Technological", "Change & Testing Management",
            "Les environnements de développement, de test et de production sont séparés afin de réduire les risques d'accès ou de modification non autorisés.",
            new int[]{5, 5, 5, 4, 5, 3, 5, 5, 5}),
        new ActionDef("A91", "ISO 8.32", "Technological", "Change & Testing Management",
            "Les changements apportés aux systèmes et applications sont planifiés, évalués, autorisés, testés et documentés de manière contrôlée.",
            new int[]{5, 5, 5, 4, 5, 4, 5, 5, 5}),
        new ActionDef("A92", "ISO 8.33", "Technological", "Change & Testing Management",
            "Les informations utilisées pour les tests sont sélectionnées, protégées et gérées afin d'éviter l'exposition ou l'utilisation non autorisée d'informations sensibles.",
            new int[]{5, 5, 4, 4, 5, 3, 5, 5, 5}),
        new ActionDef("A93", "ISO 8.34", "Technological", "Change & Testing Management",
            "Les activités d'audit et de test des systèmes d'information sont planifiées et réalisées de manière à limiter les perturbations des opérations et à protéger les informations.",
            new int[]{5, 5, 5, 4, 5, 3, 5, 5, 5})
    );

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        seedUsers();
        seedSectors();
        seedSecurityActions();
        seedActionSectorCoefficients();
    }

    private void seedUsers() {
        userRepository.findByUsername("admin").ifPresentOrElse(
                admin -> {
                    admin.setPassword(passwordEncoder.encode("admin123"));
                    userRepository.save(admin);
                },
                () -> {
                    User admin = User.builder()
                            .username("admin")
                            .password(passwordEncoder.encode("admin123"))
                            .role("ROLE_ADMIN")
                            .build();
                    userRepository.save(admin);
                    System.out.println("Seeded admin user: admin / admin123");
                }
        );

        userRepository.findByUsername("firm1").ifPresentOrElse(
                firm -> {
                    firm.setPassword(passwordEncoder.encode("firm123"));
                    if (firm.getRole() == null) firm.setRole("ROLE_USER");
                    userRepository.save(firm);
                },
                () -> {
                    User firm = User.builder()
                            .username("firm1")
                            .password(passwordEncoder.encode("firm123"))
                            .role("ROLE_USER")
                            .organizationName("Telecom Solutions Inc.")
                            .build();
                    userRepository.save(firm);
                    System.out.println("Seeded user (firm): firm1 / firm123");
                }
        );
    }

    private void seedSectors() {
        for (String sectorName : SECTORS) {
            if (!sectorRepository.existsByName(sectorName)) {
                Sector sector = Sector.builder()
                        .name(sectorName)
                        .description("Secteur d'activité : " + sectorName)
                        .build();
                sectorRepository.save(sector);
            }
        }
        System.out.println("Sectors verified/seeded: Total count in DB = " + sectorRepository.count());

        // Link existing firm1 user to an organization if not already linked
        userRepository.findByUsername("firm1").ifPresent(firmUser -> {
            if (firmUser.getOrganization() == null) {
                Sector itSector = sectorRepository.findByName("IT / Télécom").orElse(null);
                if (itSector != null) {
                    Organization defaultOrg = organizationRepository.findByName("Telecom Solutions Inc.")
                            .orElseGet(() -> {
                                Organization org = Organization.builder()
                                        .name("Telecom Solutions Inc.")
                                        .sector(itSector)
                                        .build();
                                return organizationRepository.save(org);
                            });
                    firmUser.setOrganization(defaultOrg);
                    userRepository.save(firmUser);
                    System.out.println("Linked user firm1 to Organization: Telecom Solutions Inc.");
                }
            }
        });
    }

    private void seedSecurityActions() {
        for (ActionDef def : ACTIONS) {
            if (!securityActionRepository.existsByCode(def.code)) {
                SecurityAction action = SecurityAction.builder()
                        .code(def.code)
                        .isoControl(def.isoControl)
                        .theme(def.theme)
                        .projectSubdomain(def.projectSubdomain)
                        .description(def.description)
                        .build();
                securityActionRepository.save(action);
            }
        }
        System.out.println("Security actions verified/seeded: Total count in DB = " + securityActionRepository.count());
    }

    private void seedActionSectorCoefficients() {
        // Load all sectors in a Map by name
        Map<String, Sector> sectorMap = new HashMap<>();
        for (Sector s : sectorRepository.findAll()) {
            sectorMap.put(s.getName(), s);
        }

        // Load all actions in a Map by code
        Map<String, SecurityAction> actionMap = new HashMap<>();
        for (SecurityAction a : securityActionRepository.findAll()) {
            actionMap.put(a.getCode(), a);
        }

        int newCoeffsCount = 0;
        for (ActionDef def : ACTIONS) {
            SecurityAction action = actionMap.get(def.code);
            if (action == null) continue;

            for (int i = 0; i < SECTORS.length; i++) {
                String sectorName = SECTORS[i];
                Sector sector = sectorMap.get(sectorName);
                if (sector == null) continue;

                int coeffValue = def.coefficients[i];

                if (!coefficientRepository.existsByActionAndSector(action, sector)) {
                    ActionSectorCoefficient coeff = ActionSectorCoefficient.builder()
                            .action(action)
                            .sector(sector)
                            .coefficient(coeffValue)
                            .updatedBy("SYSTEM_SEEDER")
                            .build();
                    coefficientRepository.save(coeff);
                    newCoeffsCount++;
                }
            }
        }

        if (newCoeffsCount > 0) {
            System.out.println("Seeded " + newCoeffsCount + " new action-sector coefficients.");
        }
        System.out.println("Coefficients verified/seeded: Total count in DB = " + coefficientRepository.count());
    }
}
