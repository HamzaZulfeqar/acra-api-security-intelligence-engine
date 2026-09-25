const express=require("express");
const app=express(); app.use(express.json());

app.get("/health",(req,res)=>res.json({status:"ok",framework:"express"}));
app.get("/runtime/express/assets/:assetId",(req,res)=>{
  const id=req.params.assetId, owner=id==="asset-red"?"alice":"bob", tenant=id==="asset-red"?"red":"blue";
  res.json({id,ownerId:owner,tenantId:tenant});
});
app.get("/runtime/express/workspaces/:tenant/reports/:reportId",(req,res)=>res.json({id:req.params.reportId,tenantId:req.params.tenant}));
app.get("/runtime/express/management/audit",(req,res)=>res.json({area:"audit",role:req.get("X-Role")||"",requiredRole:"security-admin"}));
app.post("/runtime/express/workflows/release/resources/:resourceId/transition",(req,res)=>res.json({...req.body,resourceId:req.params.resourceId,state:req.body.toState,role:req.get("X-Role")||""}));
app.get("/runtime/express/routing/audit",(req,res)=>res.json({area:"audit",role:req.get("X-Role")||"",routeForm:"canonical"}));
app.patch("/runtime/express/accounts/:account/preferences",(req,res)=>res.json({id:req.params.account,appliedProperties:Object.keys(req.body).sort()}));
app.post("/runtime/express/assets/bulk-read",(req,res)=>{
  const items=(req.body.resourceIds||[]).map(id=>({resourceId:id,decision:"ALLOW",ownerId:id==="asset-red"?"alice":"bob",tenantId:id==="asset-red"?"red":"blue"}));
  res.json({items});
});
app.get("/runtime/express/references/:alias",(req,res)=>{
  const id=req.params.alias==="ref-red"?"asset-red":"asset-blue", owner=id==="asset-red"?"alice":"bob", tenant=id==="asset-red"?"red":"blue";
  res.json({alias:req.params.alias,resolvedResourceId:id,resource:{id,ownerId:owner,tenantId:tenant}});
});
app.listen(Number(process.env.PORT||18203),"127.0.0.1",()=>console.log("express-runtime-ready"));
