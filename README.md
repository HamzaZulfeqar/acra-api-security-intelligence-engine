# ACRA

**API Access Control & Routing Auditor**  
**ACRA API Security Intelligence Engine**

**Canonical baseline:** RB-0  
**Current candidate:** v0.3.0-rc1  
**Current sprint:** Sprint 3, PARTIAL

ACRA is a context-aware API authorization-auditing research system. It is not a generic IDOR/BOLA payload generator and does not treat HTTP status alone as an authorization verdict.

## Overview

ACRA (API Security Intelligence Engine) is a security research and analysis framework focused on improving API authorization testing through contextual understanding, evidence-driven analysis, and intelligent risk assessment.

The platform provides capabilities for:

- API reconnaissance and endpoint intelligence
- Authorization context modeling
- BOLA and BFLA security assessment
- Differential response analysis
- Evidence chain tracking
- Security context graph modeling
- Safe active testing workflows
- Research experiment management
- Automated security validation

## Current architecture

```text
Burp / Lab / fixtures
        -> traffic transaction
        -> Security Context Engine
        -> Security Context Graph
        -> API Reconnaissance Engine
             -> semantic identifiers/parameters/headers
             -> route intelligence
             -> OpenAPI correlation/schema drift
             -> response semantics
             -> context coverage/prioritization
        -> dry-run controlled test plan
        -> active authorization testing [Sprint 4+]