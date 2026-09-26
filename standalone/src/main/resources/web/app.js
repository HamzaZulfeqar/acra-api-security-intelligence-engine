const state={
  csrf:"",
  projects:[],
  targets:[],
  inventory:[],
  context:{principals:[],roles:[],tenants:[],resources:[],expectations:[]},
  projection:null,
  evidence:{artifacts:[],samples:[]},
  candidates:[],
  coverage:{summary:{total:0,tested:0,untested:0,partial:0,inconclusive:0,notApplicable:0},entries:[]},
  active:{killSwitchEngaged:false,executions:[]},
  activeProjectId:""
};

const viewMeta={
  overview:["Overview","Local-first API authorization security analysis."],
  targets:["Targets","Register explicitly authorized HTTP(S) assessment targets."],
  inventory:["API Inventory","Import and normalize OpenAPI, HAR and raw HTTP evidence."],
  context:["Security Context","Define principals, roles, tenants, resources, ownership and expected authorization."],
  authorization:["Authorization","Identity, role, tenant and policy intelligence."],
  active:["Active Validation","Controlled loopback LAB execution through ACRA Core safety gates."],
  "object-access":["Object Access","Object-level authorization reasoning and evidence."],
  "function-access":["Function Access","Function-level authorization reasoning and evidence."],
  "property-access":["Property Access","Property-level READ/UPDATE authorization intelligence."],
  workflow:["Workflow","State transition and workflow authorization intelligence."],
  routing:["Routing","URI representation and authorization-boundary analysis."],
  evidence:["Evidence","Traceable requests, responses, observations and provenance."],
  candidates:["Candidates","Review-only security candidates and analyst decisions."],
  coverage:["Coverage","Tested, untested and inconclusive assessment coverage."],
  reports:["Reports","Deterministic evidence-backed security reporting."]
};

async function api(path,options){
  const opts=options||{};
  const headers=new Headers(opts.headers||{});
  if(state.csrf&&opts.method&&opts.method!=="GET") headers.set("X-ACRA-CSRF",state.csrf);
  const response=await fetch(path,Object.assign({},opts,{headers:headers}));
  const text=await response.text();
  let body={};
  try{body=text?JSON.parse(text):{};}catch(_){body={error:text};}
  if(!response.ok) throw new Error(body.error||("Request failed ("+response.status+")"));
  return body;
}

async function initialize(){
  try{
    const health=await api("/api/health");
    state.csrf=health.csrfToken;
    document.querySelector("#health-dot").classList.add("up");
    document.querySelector("#health-label").textContent="Local service healthy";
    const capabilities=await api("/api/capabilities");
    document.querySelector("#metric-methods").textContent=capabilities.httpMethods.length;
    await loadProjects();
    wireEvents();
  }catch(error){
    document.querySelector("#health-label").textContent="Service unavailable";
    showMessage("#project-form-message",error.message,false);
  }
}

async function loadProjects(preferId){
  state.projects=await api("/api/projects");
  document.querySelector("#metric-projects").textContent=state.projects.length;
  const selector=document.querySelector("#project-selector");
  selector.innerHTML="";

  if(!state.projects.length){
    const option=document.createElement("option");
    option.value="";
    option.textContent="No project — create one";
    selector.append(option);
    state.activeProjectId="";
    state.targets=[];
    state.inventory=[];
    state.context={principals:[],roles:[],tenants:[],resources:[],expectations:[]};
    state.projection=null;
    state.evidence={artifacts:[],samples:[]};
    state.candidates=[];
    state.coverage={summary:{total:0,tested:0,untested:0,partial:0,inconclusive:0,notApplicable:0},entries:[]};
    state.active={killSwitchEngaged:false,executions:[]};
    renderTargets();
    syncImportTargets();
    renderInventory();
    renderContext();
    renderProjection();
    renderEvidence();
    renderCandidates();
    renderCoverage();
    renderActive();
    syncActiveSelectors();
    return;
  }

  state.projects.forEach(function(project){
    const option=document.createElement("option");
    option.value=project.id;
    option.textContent=project.name;
    selector.append(option);
  });

  const validPreferred=state.projects.some(function(project){return project.id===preferId;});
  state.activeProjectId=validPreferred?preferId:state.projects[0].id;
  selector.value=state.activeProjectId;

  await loadTargets();
  await loadInventory();
  await loadContext();
  await loadProjection();
  await loadEvidence();
  await loadReviewProduct();
  await loadActive();
}

async function loadTargets(){
  if(!state.activeProjectId){
    state.targets=[];
    renderTargets();
    syncImportTargets();
    syncContextSelectors();
    return;
  }
  state.targets=await api("/api/targets?projectId="+encodeURIComponent(state.activeProjectId));
  renderTargets();
  syncImportTargets();
  syncContextSelectors();
  syncActiveSelectors();
}

async function loadInventory(){
  if(!state.activeProjectId){
    state.inventory=[];
    renderInventory();
    syncContextSelectors();
    return;
  }
  state.inventory=await api("/api/inventory?projectId="+encodeURIComponent(state.activeProjectId));
  renderInventory(document.querySelector("#inventory-search")?document.querySelector("#inventory-search").value:"");
  syncContextSelectors();
  syncActiveSelectors();
}

async function loadProjection(){
  if(!state.activeProjectId){
    state.projection=null;
    renderProjection();
    return;
  }
  state.projection=await api("/api/projection?projectId="+encodeURIComponent(state.activeProjectId));
  renderProjection(document.querySelector("#authorization-search")?document.querySelector("#authorization-search").value:"");
}

async function loadReviewProduct(){
  if(!state.activeProjectId){
    state.candidates=[];
    state.coverage={summary:{total:0,tested:0,untested:0,partial:0,inconclusive:0,notApplicable:0},entries:[]};
    renderCandidates();
    renderCoverage();
    return;
  }
  const values=await Promise.all([
    api("/api/candidates?projectId="+encodeURIComponent(state.activeProjectId)),
    api("/api/coverage?projectId="+encodeURIComponent(state.activeProjectId))
  ]);
  state.candidates=values[0];
  state.coverage=values[1];
  renderCandidates(document.querySelector("#candidate-search")?document.querySelector("#candidate-search").value:"");
  renderCoverage();
}

async function loadActive(){
  if(!state.activeProjectId){
    state.active={killSwitchEngaged:false,executions:[]};
    renderActive();
    syncActiveSelectors();
    return;
  }
  state.active=await api("/api/active?projectId="+encodeURIComponent(state.activeProjectId));
  renderActive();
  syncActiveSelectors();
}

async function loadEvidence(){
  if(!state.activeProjectId){
    state.evidence={artifacts:[],samples:[]};
    renderEvidence();
    return;
  }
  state.evidence=await api("/api/evidence?projectId="+encodeURIComponent(state.activeProjectId));
  renderEvidence(document.querySelector("#evidence-search")?document.querySelector("#evidence-search").value:"");
}

async function loadContext(){
  if(!state.activeProjectId){
    state.context={principals:[],roles:[],tenants:[],resources:[],expectations:[]};
    renderContext();
    return;
  }
  state.context=await api("/api/context?projectId="+encodeURIComponent(state.activeProjectId));
  renderContext(document.querySelector("#context-search")?document.querySelector("#context-search").value:"");
  syncContextSelectors();
  syncActiveSelectors();
}

function renderTargets(){
  const list=document.querySelector("#target-list");
  document.querySelector("#metric-targets").textContent=state.targets.length;
  document.querySelector("#target-count").textContent=state.targets.length;

  if(!state.activeProjectId){
    list.className="target-list empty-state";
    list.textContent="Create or select a project to manage targets.";
    return;
  }
  if(!state.targets.length){
    list.className="target-list empty-state";
    list.textContent="No targets registered in this project.";
    return;
  }

  list.className="target-list";
  list.innerHTML="";
  state.targets.forEach(function(target){
    const card=document.createElement("div");
    card.className="target-card";
    const title=document.createElement("strong");
    title.textContent=target.displayName;
    const url=document.createElement("code");
    url.textContent=target.baseUrl;
    const meta=document.createElement("div");
    meta.className="target-meta";

    [target.environment,target.testingMode,target.authorizationReference].forEach(function(value){
      const badge=document.createElement("span");
      badge.textContent=value;
      meta.append(badge);
    });

    card.append(title,url,meta);
    list.append(card);
  });
}

function syncImportTargets(){
  const selector=document.querySelector("#import-target");
  if(!selector) return;
  const previous=selector.value;
  selector.innerHTML="";

  if(!state.targets.length){
    const option=document.createElement("option");
    option.value="";
    option.textContent="No registered target";
    selector.append(option);
    selector.disabled=true;
    return;
  }

  selector.disabled=false;
  state.targets.forEach(function(target){
    const option=document.createElement("option");
    option.value=target.id;
    option.textContent=target.displayName+" — "+target.baseUrl;
    selector.append(option);
  });

  if(state.targets.some(function(target){return target.id===previous;})) selector.value=previous;
}

function renderInventory(filterText){
  const body=document.querySelector("#inventory-table-body");
  const empty=document.querySelector("#inventory-empty");
  if(!body||!empty) return;

  const filter=(filterText||"").trim().toLowerCase();
  const rows=state.inventory.filter(function(item){
    if(!filter) return true;
    const material=[
      item.method,item.scheme,item.host,String(item.port),item.rawPath,item.canonicalPath,
      (item.sourceTypes||[]).join(" "),(item.responseStatuses||[]).join(" "),
      item.documented?"documented":"observed"
    ].join(" ").toLowerCase();
    return material.includes(filter);
  });

  body.innerHTML="";
  rows.forEach(function(item){
    const row=document.createElement("tr");
    const values=[
      item.method,
      item.canonicalPath,
      item.scheme+"://"+item.host+":"+item.port,
      (item.sourceTypes||[]).join(", "),
      (item.responseStatuses||[]).length?(item.responseStatuses||[]).join(", "):"—",
      String(item.observationCount),
      item.documented?"Yes":"No"
    ];
    values.forEach(function(value,index){
      const cell=document.createElement("td");
      cell.textContent=value;
      if(index===0) cell.className="method-cell";
      if(index===1) cell.className="route-cell";
      row.append(cell);
    });
    body.append(row);
  });

  empty.hidden=rows.length>0;
  document.querySelector("#inventory-count").textContent=state.inventory.length;
  document.querySelector("#inventory-summary-endpoints").textContent=state.inventory.length;
  document.querySelector("#inventory-summary-documented").textContent=state.inventory.filter(function(item){return item.documented;}).length;
  document.querySelector("#inventory-summary-observed").textContent=state.inventory.filter(function(item){
    return (item.sourceTypes||[]).some(function(source){return source==="HAR"||source==="RAW_HTTP";});
  }).length;
}

function renderContext(filterText){
  const context=state.context||{principals:[],roles:[],tenants:[],resources:[],expectations:[]};
  document.querySelector("#context-principal-count").textContent=context.principals.length;
  document.querySelector("#context-role-count").textContent=context.roles.length;
  document.querySelector("#context-tenant-count").textContent=context.tenants.length;
  document.querySelector("#context-resource-count").textContent=context.resources.length;
  document.querySelector("#context-expectation-count").textContent=context.expectations.length;

  renderMiniList("#principal-list",context.principals,function(item){
    return [item.principalId,item.displayName||"Unnamed",item.authenticationType];
  });
  renderMiniList("#role-list",context.roles,function(item){return [item.roleId,item.name];});
  renderMiniList("#tenant-list",context.tenants,function(item){return [item.tenantId,item.name||item.tenantId];});
  renderMiniList("#resource-list",context.resources,function(item){
    return [
      item.resourceType+":"+item.resourceId,
      item.ownerPrincipalId?"owner="+item.ownerPrincipalId:"owner=unspecified",
      item.tenantId?"tenant="+item.tenantId:"tenant=unspecified",
      item.state||"state=unspecified"
    ];
  });

  const filter=(filterText||"").trim().toLowerCase();
  const rows=context.expectations.filter(function(item){
    if(!filter) return true;
    return [
      item.endpoint,item.action,item.principalId,item.roleId,item.tenantId,
      item.resourceId,item.expectedDecision,item.rationale
    ].join(" ").toLowerCase().includes(filter);
  });

  const body=document.querySelector("#context-table-body");
  body.innerHTML="";
  rows.forEach(function(item){
    const row=document.createElement("tr");
    [item.endpoint,item.action,item.principalId,item.roleId||"—",item.tenantId||"—",
      item.resourceId||"—",item.expectedDecision,item.rationale||"—"].forEach(function(value,index){
        const cell=document.createElement("td");
        cell.textContent=value;
        if(index===0) cell.className="route-cell";
        if(index===6) cell.className="decision-cell";
        row.append(cell);
      });
    body.append(row);
  });

  document.querySelector("#context-empty").hidden=rows.length>0;
}

function renderProjection(filterText){
  const projection=state.projection;
  document.querySelector("#auth-membership-count").textContent=projection?projection.membershipCount:0;
  document.querySelector("#auth-role-assignment-count").textContent=projection?projection.roleAssignmentCount:0;
  document.querySelector("#auth-permission-count").textContent=projection?projection.permissionCount:0;
  document.querySelector("#auth-rule-count").textContent=projection?projection.ruleCount:0;
  document.querySelector("#auth-resolution-count").textContent=projection?(projection.authorization||[]).length:0;
  document.querySelector("#auth-policy-fingerprint").textContent=projection&&projection.policyFingerprint?projection.policyFingerprint:"—";

  if(projection){
    document.querySelector("#workflow-readiness").textContent=projection.workflowResolutionCount>0
      ? projection.workflowResolutionCount+" resolution(s)"
      : "Projected · awaiting observations";
    document.querySelector("#routing-readiness").textContent=projection.routingAssessmentCount>0
      ? projection.routingAssessmentCount+" assessment(s)"
      : "Projected · awaiting observations";
    document.querySelector("#property-readiness").textContent=projection.propertyAssessmentCount>0
      ? projection.propertyAssessmentCount+" assessment(s)"
      : "Projected · awaiting observations";
  }else{
    document.querySelector("#workflow-readiness").textContent="Not loaded";
    document.querySelector("#routing-readiness").textContent="Not loaded";
    document.querySelector("#property-readiness").textContent="Not loaded";
  }

  const body=document.querySelector("#authorization-table-body");
  const empty=document.querySelector("#authorization-empty");
  if(!body||!empty) return;
  const filter=(filterText||"").trim().toLowerCase();
  const rows=(projection&&projection.authorization?projection.authorization:[]).filter(function(item){
    if(!filter) return true;
    return [
      item.endpoint,item.action,item.principalId,item.configuredDecision,item.resolvedDecision,
      item.resolutionState,(item.effectiveRoleIds||[]).join(" "),item.bolaStatus,item.bflaStatus,
      (item.resolutionReasons||[]).join(" ")
    ].join(" ").toLowerCase().includes(filter);
  });

  body.innerHTML="";
  rows.forEach(function(item){
    const row=document.createElement("tr");
    [
      item.endpoint,
      item.action,
      item.principalId,
      item.configuredDecision,
      item.resolvedDecision,
      item.resolutionState,
      (item.effectiveRoleIds||[]).join(", ")||"—",
      item.bolaStatus,
      item.bflaStatus,
      (item.resolutionReasons||[]).join(", ")||"—"
    ].forEach(function(value,index){
      const cell=document.createElement("td");
      cell.textContent=value;
      if(index===0) cell.className="route-cell";
      if(index===3||index===4) cell.className="decision-cell";
      row.append(cell);
    });
    body.append(row);
  });
  empty.hidden=rows.length>0;
}

function renderEvidence(filterText){
  const evidence=state.evidence||{artifacts:[],samples:[]};
  const artifacts=evidence.artifacts||[];
  const samples=evidence.samples||[];
  document.querySelector("#evidence-artifact-count").textContent=artifacts.length;
  document.querySelector("#evidence-sample-count").textContent=samples.length;
  document.querySelector("#evidence-redacted-count").textContent=artifacts.filter(function(item){return item.redactionApplied;}).length;
  document.querySelector("#evidence-response-count").textContent=samples.filter(function(item){return item.hasResponse;}).length;

  const list=document.querySelector("#evidence-artifact-list");
  list.innerHTML="";
  if(!artifacts.length){
    const empty=document.createElement("div");
    empty.className="mini-empty";
    empty.textContent="No evidence archived";
    list.append(empty);
  }else{
    artifacts.forEach(function(item){
      const row=document.createElement("div");
      row.className="mini-item evidence-artifact-item";
      const strong=document.createElement("strong");
      strong.textContent=item.evidenceType+" · "+(item.sourceReference||item.evidenceId);
      const digest=document.createElement("span");
      digest.textContent="sha256 "+item.originalSha256.slice(0,20)+"… · samples "+item.httpSampleCount+(item.redactionApplied?" · redacted":"");
      const button=document.createElement("button");
      button.type="button";
      button.className="button secondary compact-button";
      button.textContent="View Redacted";
      button.addEventListener("click",function(){viewEvidenceArtifact(item.evidenceId);});
      row.append(strong,digest,button);
      list.append(row);
    });
  }

  const filter=(filterText||"").trim().toLowerCase();
  const artifactById=new Map(artifacts.map(function(item){return [item.evidenceId,item];}));
  const rows=samples.filter(function(item){
    if(!filter) return true;
    const artifact=artifactById.get(item.evidenceId);
    return [
      item.method,item.requestUrl,String(item.responseStatus),item.responseContentType,
      artifact?artifact.evidenceType:"",artifact?artifact.sourceReference:""
    ].join(" ").toLowerCase().includes(filter);
  });

  const body=document.querySelector("#evidence-sample-table-body");
  body.innerHTML="";
  rows.forEach(function(item){
    const artifact=artifactById.get(item.evidenceId);
    const row=document.createElement("tr");
    [
      item.method||"—",
      item.requestUrl||"—",
      item.hasResponse?String(item.responseStatus):"—",
      item.responseContentType||"—",
      artifact?(artifact.evidenceType+" · "+(artifact.sourceReference||artifact.evidenceId)):item.evidenceId,
      item.createdAt
    ].forEach(function(value,index){
      const cell=document.createElement("td");
      cell.textContent=value;
      if(index===0) cell.className="method-cell";
      if(index===1) cell.className="route-cell";
      row.append(cell);
    });
    body.append(row);
  });
  document.querySelector("#evidence-empty").hidden=rows.length>0;
  syncDifferentialSelectors();
}

function syncDifferentialSelectors(){
  const responseSamples=(state.evidence.samples||[]).filter(function(item){return item.hasResponse;});
  setOptions("#http-diff-left",responseSamples,function(item){return item.sampleId;},
    function(item){return item.method+" "+item.requestUrl+" → "+item.responseStatus;},false);
  setOptions("#http-diff-right",responseSamples,function(item){return item.sampleId;},
    function(item){return item.method+" "+item.requestUrl+" → "+item.responseStatus;},false);

  const expectations=(state.context&&state.context.expectations)||[];
  setOptions("#auth-diff-left",expectations,function(item){return item.id;},
    function(item){return item.principalId+" · "+item.action+" "+item.endpoint+" → "+item.expectedDecision;},false);
  setOptions("#auth-diff-right",expectations,function(item){return item.id;},
    function(item){return item.principalId+" · "+item.action+" "+item.endpoint+" → "+item.expectedDecision;},false);
}

async function viewEvidenceArtifact(evidenceId){
  try{
    const detail=await api("/api/evidence?projectId="+encodeURIComponent(state.activeProjectId)+"&evidenceId="+encodeURIComponent(evidenceId));
    document.querySelector("#evidence-preview").textContent=detail.redactedContent||"(empty redacted evidence)";
  }catch(error){
    document.querySelector("#evidence-preview").textContent="Unable to load evidence: "+error.message;
  }
}

function renderCandidates(filterText){
  const host=document.querySelector("#candidate-list");
  if(!host) return;
  const filter=(filterText||"").trim().toLowerCase();
  const rows=(state.candidates||[]).filter(function(item){
    if(!filter) return true;
    return [
      item.candidateId,item.endpoint,item.principalId,item.resourceId,item.coreState,item.reviewState,
      item.expectedDecision,item.observedDecision,item.confidence,item.rationale,item.reviewNote
    ].join(" ").toLowerCase().includes(filter);
  });
  host.innerHTML="";
  if(!rows.length){
    const empty=document.createElement("div");
    empty.className="empty-state";
    empty.textContent="No review candidates are available.";
    host.append(empty);
    return;
  }

  rows.forEach(function(item){
    const card=document.createElement("article");
    card.className="candidate-card";
    const heading=document.createElement("div");
    heading.className="candidate-heading";
    const title=document.createElement("strong");
    title.textContent=item.endpoint+" · "+item.principalId;
    const stateBadge=document.createElement("span");
    stateBadge.className="status-badge";
    stateBadge.textContent=item.coreState;
    heading.append(title,stateBadge);

    const meta=document.createElement("div");
    meta.className="candidate-meta";
    [
      "Expected: "+item.expectedDecision,
      "Observed: "+item.observedDecision,
      "Confidence: "+item.confidence,
      "Resource: "+(item.resourceId||"—")
    ].forEach(function(value){const span=document.createElement("span");span.textContent=value;meta.append(span);});

    const form=document.createElement("form");
    form.className="candidate-review-form";
    form.dataset.candidateId=item.candidateId;
    const select=document.createElement("select");
    select.name="state";
    ["NEW","UNDER_REVIEW","NEEDS_MORE_EVIDENCE","REJECTED"].forEach(function(value){
      const option=document.createElement("option");
      option.value=value;option.textContent=value.replaceAll("_"," ");
      option.selected=value===item.reviewState;
      select.append(option);
    });
    const note=document.createElement("input");
    note.name="note";
    note.maxLength=1000;
    note.placeholder="Analyst note (no secrets)";
    note.value=item.reviewNote||"";
    const button=document.createElement("button");
    button.type="submit";button.className="button secondary";button.textContent="Save Review";
    form.append(select,note,button);
    form.addEventListener("submit",saveCandidateReview);

    const rationale=document.createElement("p");
    rationale.className="candidate-rationale";
    rationale.textContent=item.rationale;
    card.append(heading,meta,rationale,form);
    host.append(card);
  });
}

function renderCoverage(){
  const data=state.coverage||{summary:{},entries:[]};
  const summary=data.summary||{};
  document.querySelector("#coverage-total").textContent=summary.total||0;
  document.querySelector("#coverage-tested").textContent=summary.tested||0;
  document.querySelector("#coverage-untested").textContent=summary.untested||0;
  document.querySelector("#coverage-partial").textContent=summary.partial||0;
  document.querySelector("#coverage-inconclusive").textContent=summary.inconclusive||0;

  const body=document.querySelector("#coverage-table-body");
  if(!body) return;
  body.innerHTML="";
  (data.entries||[]).forEach(function(item){
    const row=document.createElement("tr");
    [item.method,item.endpoint,item.disposition,String(item.expectationCount),String(item.passiveObservationCount),item.reason]
      .forEach(function(value,index){
        const cell=document.createElement("td");
        cell.textContent=value;
        if(index===0) cell.className="method-cell";
        if(index===1) cell.className="route-cell";
        row.append(cell);
      });
    body.append(row);
  });
  document.querySelector("#coverage-empty").hidden=(data.entries||[]).length>0;
}

function renderActive(){
  const data=state.active||{killSwitchEngaged:false,executions:[]};
  const status=document.querySelector("#active-kill-status");
  const submit=document.querySelector("#active-form button[type='submit']");
  if(status){
    status.textContent=data.killSwitchEngaged?"Kill switch: ENGAGED":"Kill switch: ready";
    status.classList.toggle("active-kill-engaged",Boolean(data.killSwitchEngaged));
  }
  if(submit) submit.disabled=Boolean(data.killSwitchEngaged);

  const executions=data.executions||[];
  const count=document.querySelector("#active-execution-count");
  if(count) count.textContent=executions.length;
  const body=document.querySelector("#active-table-body");
  const empty=document.querySelector("#active-empty");
  if(!body||!empty) return;
  body.innerHTML="";
  executions.slice().reverse().forEach(function(item){
    const row=document.createElement("tr");
    [
      item.endpoint,
      item.requestPath,
      item.mutatedPath,
      item.expectedDecision,
      item.observedDecision,
      item.differentialClassification,
      item.state,
      String(item.coreEvidenceObjectCount),
      item.createdAt
    ].forEach(function(value,index){
      const cell=document.createElement("td");
      cell.textContent=value;
      if(index===0||index===1||index===2) cell.className="route-cell";
      if(index===3||index===4) cell.classList.add("decision-cell");
      row.append(cell);
    });
    body.append(row);
  });
  empty.hidden=executions.length>0;
}

function isLoopbackControlledTarget(target){
  if(!target||target.environment!=="LAB"||target.testingMode!=="CONTROLLED_LAB") return false;
  try{
    const host=new URL(target.baseUrl).hostname.toLowerCase();
    return host==="localhost"||host==="127.0.0.1"||host==="::1"||host==="[::1]";
  }catch(_){
    return false;
  }
}

function syncActiveSelectors(){
  const targetSelect=document.querySelector("#active-target");
  const expectationSelect=document.querySelector("#active-expectation");
  if(!targetSelect||!expectationSelect) return;

  const previousTarget=targetSelect.value;
  const targets=state.targets.filter(isLoopbackControlledTarget);
  setOptions("#active-target",targets,function(item){return item.id;},
    function(item){return item.displayName+" — "+item.baseUrl;},false);
  if(targets.some(function(item){return item.id===previousTarget;})) targetSelect.value=previousTarget;
  targetSelect.disabled=targets.length===0;

  syncActiveExpectations();
}

function syncActiveExpectations(){
  const targetSelect=document.querySelector("#active-target");
  const expectationSelect=document.querySelector("#active-expectation");
  if(!targetSelect||!expectationSelect) return;
  const targetId=targetSelect.value;
  const safeEndpointKeys=new Set(
    state.inventory
      .filter(function(item){
        return item.targetId===targetId&&["GET","HEAD","OPTIONS"].includes(item.method);
      })
      .map(function(item){return item.canonicalPath;})
  );
  const rows=(state.context.expectations||[]).filter(function(item){
    return item.targetId===targetId
      && item.action==="READ"
      && (item.expectedDecision==="ALLOW"||item.expectedDecision==="DENY")
      && safeEndpointKeys.has(item.endpoint);
  });
  setOptions("#active-expectation",rows,function(item){return item.id;},
    function(item){
      return item.principalId+" · "+item.expectedDecision+" · "+item.endpoint;
    },false);
  expectationSelect.disabled=rows.length===0;
  syncActivePath();
}

function syncActivePath(){
  const targetId=document.querySelector("#active-target")?.value||"";
  const expectationId=document.querySelector("#active-expectation")?.value||"";
  const input=document.querySelector("#active-path");
  if(!input) return;
  const expectation=(state.context.expectations||[]).find(function(item){return item.id===expectationId;});
  if(!expectation){input.value="";return;}
  const candidates=state.inventory.filter(function(item){
    return item.targetId===targetId
      && item.canonicalPath===expectation.endpoint
      && ["GET","HEAD","OPTIONS"].includes(item.method)
      && !item.rawPath.includes("{")
      && !item.rawPath.includes("}");
  });
  if(candidates.length) input.value=candidates[0].rawPath;
  else if(!input.value||input.dataset.autoFilled==="true") input.value="";
  input.dataset.autoFilled=candidates.length?"true":"false";
}

function renderMiniList(selector,items,formatter){
  const host=document.querySelector(selector);
  if(!host) return;
  host.innerHTML="";
  if(!items.length){
    const empty=document.createElement("div");
    empty.className="mini-empty";
    empty.textContent="None defined";
    host.append(empty);
    return;
  }
  items.forEach(function(item){
    const row=document.createElement("div");
    row.className="mini-item";
    const values=formatter(item);
    const strong=document.createElement("strong");
    strong.textContent=values[0];
    row.append(strong);
    values.slice(1).forEach(function(value){
      const span=document.createElement("span");
      span.textContent=value;
      row.append(span);
    });
    host.append(row);
  });
}

function setOptions(selectorId,items,valueFn,labelFn,allowBlank){
  const selector=document.querySelector(selectorId);
  if(!selector) return;
  const previous=selector.value;
  selector.innerHTML="";
  if(allowBlank){
    const blank=document.createElement("option");
    blank.value="";
    blank.textContent="Unspecified";
    selector.append(blank);
  }
  items.forEach(function(item){
    const option=document.createElement("option");
    option.value=valueFn(item);
    option.textContent=labelFn(item);
    selector.append(option);
  });
  if(Array.from(selector.options).some(function(option){return option.value===previous;})) selector.value=previous;
}

function syncContextSelectors(){
  const context=state.context||{principals:[],roles:[],tenants:[],resources:[],expectations:[]};

  setOptions("#resource-owner",context.principals,function(item){return item.principalId;},
    function(item){return item.principalId+" — "+(item.displayName||"Unnamed");},true);
  setOptions("#resource-tenant",context.tenants,function(item){return item.tenantId;},
    function(item){return item.tenantId+" — "+(item.name||item.tenantId);},true);

  setOptions("#expectation-principal",context.principals,function(item){return item.principalId;},
    function(item){return item.principalId+" — "+(item.displayName||"Unnamed");},false);
  setOptions("#expectation-role",context.roles,function(item){return item.roleId;},
    function(item){return item.roleId+" — "+item.name;},true);
  setOptions("#expectation-tenant",context.tenants,function(item){return item.tenantId;},
    function(item){return item.tenantId+" — "+(item.name||item.tenantId);},true);
  setOptions("#expectation-resource",context.resources,function(item){return item.resourceId;},
    function(item){return item.resourceType+":"+item.resourceId;},true);

  setOptions("#expectation-target",state.targets,function(item){return item.id;},
    function(item){return item.displayName+" — "+item.baseUrl;},false);
  syncExpectationEndpoints();
}

function syncExpectationEndpoints(){
  const targetSelector=document.querySelector("#expectation-target");
  const endpointSelector=document.querySelector("#expectation-endpoint");
  if(!targetSelector||!endpointSelector) return;

  const targetId=targetSelector.value;
  const endpoints=state.inventory.filter(function(item){return !targetId||item.targetId===targetId;});
  setOptions("#expectation-endpoint",endpoints,function(item){return item.canonicalPath;},
    function(item){return item.method+" "+item.canonicalPath;},false);
}

function wireEvents(){
  document.querySelectorAll(".nav-item").forEach(function(button){
    button.addEventListener("click",function(){openView(button.dataset.view);});
  });
  document.querySelectorAll("[data-open-targets]").forEach(function(button){
    button.addEventListener("click",function(){openView("targets");});
  });

  const dialog=document.querySelector("#project-dialog");
  document.querySelector("#new-project-button").addEventListener("click",function(){dialog.showModal();});
  document.querySelector("#close-project-dialog").addEventListener("click",function(){dialog.close();});
  document.querySelector("#cancel-project-dialog").addEventListener("click",function(){dialog.close();});

  document.querySelector("#project-selector").addEventListener("change",async function(event){
    state.activeProjectId=event.target.value;
    await loadTargets();
    await loadInventory();
    await loadContext();
    await loadProjection();
    await loadEvidence();
    await loadReviewProduct();
    await loadActive();
  });

  document.querySelector("#project-form").addEventListener("submit",createProject);
  document.querySelector("#target-form").addEventListener("submit",addTarget);
  document.querySelector("#import-form").addEventListener("submit",importEvidence);
  document.querySelector("#principal-form").addEventListener("submit",addPrincipal);
  document.querySelector("#role-form").addEventListener("submit",addRole);
  document.querySelector("#tenant-form").addEventListener("submit",addTenant);
  document.querySelector("#resource-form").addEventListener("submit",addResource);
  document.querySelector("#expectation-form").addEventListener("submit",addExpectation);

  document.querySelector("#inventory-search").addEventListener("input",function(event){
    renderInventory(event.target.value);
  });
  document.querySelector("#context-search").addEventListener("input",function(event){
    renderContext(event.target.value);
  });
  document.querySelector("#authorization-search").addEventListener("input",function(event){
    renderProjection(event.target.value);
  });
  document.querySelector("#evidence-search").addEventListener("input",function(event){
    renderEvidence(event.target.value);
  });
  document.querySelector("#candidate-search").addEventListener("input",function(event){
    renderCandidates(event.target.value);
  });
  document.querySelector("#report-form").addEventListener("submit",generateReport);
  document.querySelector("#http-diff-form").addEventListener("submit",compareHttpEvidence);
  document.querySelector("#auth-diff-form").addEventListener("submit",compareAuthorizationEvidence);
  document.querySelector("#expectation-target").addEventListener("change",syncExpectationEndpoints);
  document.querySelector("#active-target").addEventListener("change",syncActiveExpectations);
  document.querySelector("#active-expectation").addEventListener("change",syncActivePath);
  document.querySelector("#active-form").addEventListener("submit",runActiveValidation);
  document.querySelector("#active-kill-button").addEventListener("click",engageActiveKillSwitch);
  document.querySelector("#active-reset-button").addEventListener("click",resetActiveKillSwitch);

  document.querySelector("#import-file").addEventListener("change",async function(event){
    const file=event.target.files&&event.target.files[0];
    if(!file) return;
    if(file.size>2*1024*1024){
      showMessage("#import-message","File exceeds the 2 MiB import content limit.",false);
      event.target.value="";
      return;
    }
    document.querySelector("#import-content").value=await file.text();
    const source=document.querySelector("#import-form input[name='sourceReference']");
    if(!source.value) source.value=file.name;
  });
}

function openView(name){
  document.querySelectorAll(".nav-item").forEach(function(item){item.classList.toggle("active",item.dataset.view===name);});
  document.querySelectorAll(".view").forEach(function(view){view.classList.remove("active");});
  const meta=viewMeta[name]||[name,""];
  document.querySelector("#view-title").textContent=meta[0];
  document.querySelector("#view-subtitle").textContent=meta[1];

  if(name==="overview"||name==="targets"||name==="inventory"||name==="context"||name==="authorization"||name==="active"||name==="evidence"||name==="candidates"||name==="coverage"||name==="reports"){
    document.querySelector("#"+name+"-view").classList.add("active");
  }else{
    document.querySelector("#placeholder-title").textContent=meta[0];
    document.querySelector("#placeholder-copy").textContent=meta[1];
    document.querySelector("#placeholder-view").classList.add("active");
  }
}

async function createProject(event){
  event.preventDefault();
  const form=event.currentTarget;
  const body=new URLSearchParams(new FormData(form));
  try{
    const project=await api("/api/projects",{method:"POST",headers:{"Content-Type":"application/x-www-form-urlencoded"},body:body});
    form.reset();
    document.querySelector("#project-dialog").close();
    await loadProjects(project.id);
    openView("targets");
  }catch(error){showMessage("#project-form-message",error.message,false);}
}

async function addTarget(event){
  event.preventDefault();
  if(!state.activeProjectId){
    showMessage("#target-form-message","Create a project before adding targets.",false);
    return;
  }
  const form=event.currentTarget;
  const body=new URLSearchParams(new FormData(form));
  body.set("projectId",state.activeProjectId);
  try{
    await api("/api/targets",{method:"POST",headers:{"Content-Type":"application/x-www-form-urlencoded"},body:body});
    form.reset();
    showMessage("#target-form-message","Authorized target registered. No scan was started.",true);
    await loadTargets();
  }catch(error){showMessage("#target-form-message",error.message,false);}
}

async function importEvidence(event){
  event.preventDefault();
  if(!state.activeProjectId){
    showMessage("#import-message","Create a project before importing evidence.",false);
    return;
  }
  const form=event.currentTarget;
  const body=new URLSearchParams(new FormData(form));
  body.set("projectId",state.activeProjectId);
  body.set("content",document.querySelector("#import-content").value);
  try{
    const summary=await api("/api/import",{method:"POST",headers:{"Content-Type":"application/x-www-form-urlencoded"},body:body});
    showMessage("#import-message",
      summary.importType+" import complete: "+summary.observations+" observation(s), "+
      summary.uniqueEndpoints+" unique endpoint(s), "+summary.inventorySize+" total inventory endpoint(s).",true);
    await loadInventory();
    await loadEvidence();
    await loadReviewProduct();
  }catch(error){
    showMessage("#import-message",error.message,false);
  }
}

async function postContext(kind,form,messageSelector){
  if(!state.activeProjectId){
    showMessage(messageSelector,"Create a project first.",false);
    return null;
  }
  const body=new URLSearchParams(new FormData(form));
  body.set("projectId",state.activeProjectId);
  body.set("kind",kind);
  try{
    const created=await api("/api/context",{method:"POST",headers:{"Content-Type":"application/x-www-form-urlencoded"},body:body});
    form.reset();
    showMessage(messageSelector,kind.charAt(0)+kind.slice(1).toLowerCase()+" saved.",true);
    await loadContext();
    await loadProjection();
    renderEvidence(document.querySelector("#evidence-search").value);
    await loadReviewProduct();
    return created;
  }catch(error){
    showMessage(messageSelector,error.message,false);
    return null;
  }
}

async function addPrincipal(event){event.preventDefault();await postContext("PRINCIPAL",event.currentTarget,"#principal-message");}
async function addRole(event){event.preventDefault();await postContext("ROLE",event.currentTarget,"#role-tenant-message");}
async function addTenant(event){event.preventDefault();await postContext("TENANT",event.currentTarget,"#role-tenant-message");}
async function addResource(event){event.preventDefault();await postContext("RESOURCE",event.currentTarget,"#resource-message");}
async function addExpectation(event){event.preventDefault();await postContext("EXPECTATION",event.currentTarget,"#expectation-message");}

async function compareHttpEvidence(event){
  event.preventDefault();
  const form=new FormData(event.currentTarget);
  const leftId=form.get("leftId");
  const rightId=form.get("rightId");
  if(!leftId||!rightId){
    document.querySelector("#http-diff-result").textContent="Two response samples are required.";
    return;
  }
  try{
    const diff=await api("/api/differential?projectId="+encodeURIComponent(state.activeProjectId)
      +"&type=HTTP&leftId="+encodeURIComponent(leftId)
      +"&rightId="+encodeURIComponent(rightId)
      +"&mode="+encodeURIComponent(form.get("mode")||"NORMALIZED"));
    document.querySelector("#http-diff-result").textContent=
      (diff.equivalent?"Equivalent":"Different")+" · status "+diff.leftStatus+" → "+diff.rightStatus+
      " · changed: "+((diff.changedSignals||[]).join(", ")||"none")+
      " · same method: "+diff.requestMethodEqual+" · same URL: "+diff.requestUrlEqual;
  }catch(error){
    document.querySelector("#http-diff-result").textContent="Comparison failed: "+error.message;
  }
}

async function compareAuthorizationEvidence(event){
  event.preventDefault();
  const form=new FormData(event.currentTarget);
  const leftId=form.get("leftId");
  const rightId=form.get("rightId");
  if(!leftId||!rightId){
    document.querySelector("#auth-diff-result").textContent="Two authorization contexts are required.";
    return;
  }
  try{
    const diff=await api("/api/differential?projectId="+encodeURIComponent(state.activeProjectId)
      +"&type=AUTHORIZATION&leftId="+encodeURIComponent(leftId)
      +"&rightId="+encodeURIComponent(rightId));
    document.querySelector("#auth-diff-result").textContent=
      (diff.equivalent?"Equivalent contexts":"Context differences")+" · changed: "+
      ((diff.changedFields||[]).join(", ")||"none");
  }catch(error){
    document.querySelector("#auth-diff-result").textContent="Comparison failed: "+error.message;
  }
}

async function saveCandidateReview(event){
  event.preventDefault();
  const form=event.currentTarget;
  const body=new URLSearchParams(new FormData(form));
  body.set("projectId",state.activeProjectId);
  body.set("candidateId",form.dataset.candidateId);
  try{
    await api("/api/candidates",{method:"POST",headers:{"Content-Type":"application/x-www-form-urlencoded"},body:body});
    await loadReviewProduct();
  }catch(error){
    window.alert("Review update failed: "+error.message);
  }
}

async function generateReport(event){
  event.preventDefault();
  const form=new FormData(event.currentTarget);
  try{
    const artifact=await api("/api/report?projectId="+encodeURIComponent(state.activeProjectId)
      +"&format="+encodeURIComponent(form.get("format")||"JSON"));
    document.querySelector("#report-sha").textContent=artifact.sha256;
    document.querySelector("#report-preview").textContent=artifact.content;
  }catch(error){
    document.querySelector("#report-preview").textContent="Report generation failed: "+error.message;
    document.querySelector("#report-sha").textContent="—";
  }
}

async function runActiveValidation(event){
  event.preventDefault();
  if(!state.activeProjectId){
    showMessage("#active-message","Create or select a project first.",false);
    return;
  }
  const form=event.currentTarget;
  const body=new URLSearchParams(new FormData(form));
  body.set("projectId",state.activeProjectId);
  body.set("action","EXECUTE_ROUTE_EQUIVALENCE");
  body.set("confirmed",form.elements.confirmed.checked?"true":"false");
  try{
    const result=await api("/api/active",{
      method:"POST",
      headers:{"Content-Type":"application/x-www-form-urlencoded"},
      body:body
    });
    form.elements.testedAuthorizationValue.value="";
    form.elements.positiveControlAuthorizationValue.value="";
    form.elements.confirmed.checked=false;
    showMessage("#active-message",
      "Controlled execution complete: expected "+result.expectedDecision+
      ", observed "+result.observedDecision+
      ", differential "+result.differentialClassification+".",true);
    await loadActive();
    await loadEvidence();
    await loadReviewProduct();
  }catch(error){
    form.elements.testedAuthorizationValue.value="";
    form.elements.positiveControlAuthorizationValue.value="";
    form.elements.confirmed.checked=false;
    showMessage("#active-message",error.message,false);
    await loadActive();
  }
}

async function engageActiveKillSwitch(){
  if(!state.activeProjectId) return;
  try{
    const body=new URLSearchParams();
    body.set("action","KILL");
    body.set("reason","standalone operator stop");
    await api("/api/active",{method:"POST",headers:{"Content-Type":"application/x-www-form-urlencoded"},body:body});
    showMessage("#active-message","Kill switch engaged. Controlled active execution is blocked.",true);
    await loadActive();
  }catch(error){
    showMessage("#active-message",error.message,false);
  }
}

async function resetActiveKillSwitch(){
  if(!state.activeProjectId) return;
  if(!window.confirm("Reset the ACRA kill switch and re-enable controlled localhost execution?")) return;
  try{
    const body=new URLSearchParams();
    body.set("action","RESET_KILL");
    body.set("confirmed","true");
    body.set("reason","standalone operator reset");
    await api("/api/active",{method:"POST",headers:{"Content-Type":"application/x-www-form-urlencoded"},body:body});
    showMessage("#active-message","Kill switch reset. Guarded controlled execution is available.",true);
    await loadActive();
  }catch(error){
    showMessage("#active-message",error.message,false);
  }
}

function showMessage(selector,text,success){
  const element=document.querySelector(selector);
  element.hidden=false;
  element.textContent=text;
  element.classList.toggle("success",Boolean(success));
}

initialize();
