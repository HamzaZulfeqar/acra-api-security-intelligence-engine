const state={csrf:"",projects:[],targets:[],activeProjectId:""};

const viewMeta={
  overview:["Overview","Local-first API authorization security analysis."],
  targets:["Targets","Register explicitly authorized HTTP(S) assessment targets."],
  inventory:["API Inventory","Canonical endpoint and request surface intelligence."],
  authorization:["Authorization","Identity, role, tenant and policy intelligence."],
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
    renderTargets();
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
}

async function loadTargets(){
  if(!state.activeProjectId){
    state.targets=[];
    renderTargets();
    return;
  }
  state.targets=await api("/api/targets?projectId="+encodeURIComponent(state.activeProjectId));
  renderTargets();
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
  });
  document.querySelector("#project-form").addEventListener("submit",createProject);
  document.querySelector("#target-form").addEventListener("submit",addTarget);
}

function openView(name){
  document.querySelectorAll(".nav-item").forEach(function(item){item.classList.toggle("active",item.dataset.view===name);});
  document.querySelectorAll(".view").forEach(function(view){view.classList.remove("active");});
  const meta=viewMeta[name]||[name,""];
  document.querySelector("#view-title").textContent=meta[0];
  document.querySelector("#view-subtitle").textContent=meta[1];
  if(name==="overview"||name==="targets"){
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

function showMessage(selector,text,success){
  const element=document.querySelector(selector);
  element.hidden=false;
  element.textContent=text;
  element.classList.toggle("success",Boolean(success));
}

initialize();
