#!/usr/bin/env python3
import base64, json, os, re, time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse, parse_qs

MODE=os.environ.get('ACRA_LAB_MODE','secure').lower()
PORT=int(os.environ.get('PORT','18082' if MODE=='secure' else '18081'))
DOCS={
 '1001':{'id':'1001','tenant_id':'tenant-a','owner_id':'user-a','title':'Tenant A design','state':'draft'},
 '1002':{'id':'1002','tenant_id':'tenant-a','owner_id':'user-a','title':'Tenant A notes','state':'review'},
 '2001':{'id':'2001','tenant_id':'tenant-b','owner_id':'user-b','title':'Tenant B plan','state':'draft'},
}
USERS={
 'user-a':{'id':'user-a','tenant_id':'tenant-a','role':'viewer'},
 'user-b':{'id':'user-b','tenant_id':'tenant-b','role':'editor'},
 'admin-a':{'id':'admin-a','tenant_id':'tenant-a','role':'admin'},
 'manager-a':{'id':'manager-a','tenant_id':'tenant-a','role':'manager'},
 'global-admin':{'id':'global-admin','tenant_id':'global','role':'global-admin'},
 'delegate-a':{'id':'delegate-a','tenant_id':'tenant-a','role':'delegated-admin'},
 'auditor-a':{'id':'auditor-a','tenant_id':'tenant-a','role':'auditor'},
}
COMMENTS={'1001':[{'id':'c-1','document_id':'1001','owner_id':'user-a','tenant_id':'tenant-a','text':'review me'}]}
S4_DOCS={
 'Document-A':{'id':'Document-A','tenant_id':'tenant-a','owner_id':'user-a','title':'S4 owned by A'},
 'Document-B':{'id':'Document-B','tenant_id':'tenant-b','owner_id':'user-b','title':'S4 owned by B'},
}
DOC_RE=re.compile(r'^/api/v(?P<version>[12])/tenants/(?P<tenant>[^/]+)/documents/(?P<doc>[^/?]+)$')
DOC_LIST_RE=re.compile(r'^/api/v1/tenants/(?P<tenant>[^/]+)/documents$')
TENANT_USER_RE=re.compile(r'^/api/v1/tenants/(?P<tenant>[^/]+)/users/(?P<user>[^/?]+)$')
USER_RE=re.compile(r'^/api/v1/users/(?P<user>[^/?]+)$')
APPROVE_RE=re.compile(r'^/api/v1/tenants/(?P<tenant>[^/]+)/documents/(?P<doc>[^/?]+)/approve$')
COMMENTS_RE=re.compile(r'^/api/v1/tenants/(?P<tenant>[^/]+)/documents/(?P<doc>[^/?]+)/comments$')
S4_DOC_RE=re.compile(r'^/api/v1/s4/documents/(?P<doc>Document-[AB])$')
S6_REPORTS={
 'report-a':{'id':'report-a','tenant_id':'tenant-a','owner_id':'manager-a','classification':'internal'},
 'report-b':{'id':'report-b','tenant_id':'tenant-b','owner_id':'user-b','classification':'internal'},
 'shared-report':{'id':'shared-report','tenant_id':'shared','owner_id':'system','classification':'shared'},
}
S6_TENANT_REPORTS={
 'tenant-a':{'report-common':{'id':'report-common','tenant_id':'tenant-a','owner_id':'manager-a','classification':'internal'}},
 'tenant-b':{'report-common':{'id':'report-common','tenant_id':'tenant-b','owner_id':'user-b','classification':'internal'}},
}
S6_REPORT_RE=re.compile(r'^/api/v1/s6/tenants/(?P<tenant>[^/]+)/reports/(?P<report>[^/?]+)$')
S6_EXPORT_RE=re.compile(r'^/api/v1/s6/tenants/(?P<tenant>[^/]+)/admin/export$')
S6_ADMIN_SUMMARY_RE=re.compile(r'^/api/v1/s6/tenants/(?P<tenant>[^/]+)/admin/summary$')
S7_WORKFLOW_TRANSITION_RE=re.compile(r'^/api/v1/s7/workflows/(?P<workflow>[^/]+)/resources/(?P<resource>[^/]+)/transition$')
S9_PROFILE_RE=re.compile(r'^/api/v1/s9/users/(?P<user>[^/]+)/profile$')
S9_PROFILES={
 'user-a':{'id':'user-a','display_name':'User A','tenant_id':'tenant-a','is_admin':False,'salary_band':'L2'},
 'user-b':{'id':'user-b','display_name':'User B','tenant_id':'tenant-b','is_admin':False,'salary_band':'L3'},
}
def claim(token,name):
    try:
        part=token.split('.')[1]
        part += '='*((4-len(part)%4)%4)
        obj=json.loads(base64.urlsafe_b64decode(part.encode()).decode())
        return obj.get(name)
    except Exception:
        return None

class Handler(BaseHTTPRequestHandler):
    server_version='ACRA-Lab/0.3'
    def log_message(self, fmt, *args):
        pass
    def _json(self,status,obj,extra_headers=None,pretty=False):
        data=json.dumps(obj,indent=2 if pretty else None,separators=None if pretty else (',',':')).encode()
        self.send_response(status)
        self.send_header('Content-Type','application/json')
        self.send_header('Content-Length',str(len(data)))
        self.send_header('X-Request-ID',f'lab-{id(self)}-{time.time_ns()}')
        for name,value in (extra_headers or {}).items(): self.send_header(name,value)
        self.end_headers()
        try: self.wfile.write(data)
        except (BrokenPipeError,ConnectionResetError): pass
    def _identity(self):
        auth=self.headers.get('Authorization','')
        if not auth.lower().startswith('bearer '): return None
        token=auth[7:].strip()
        delegated=claim(token,'delegated_tenants')
        if not isinstance(delegated,list): delegated=[]
        return {'sub':claim(token,'sub'),'tenant_id':claim(token,'tenant_id'),'role':claim(token,'role'),
                'delegated_tenants':delegated}
    def _require_identity(self):
        ident=self._identity()
        if ident is None or not ident.get('sub'):
            self._json(401,{'error':'unauthorized'}); return None
        return ident
    def _s6_tenant_allowed(self,ident,target_tenant):
        if target_tenant=='shared': return True
        if ident.get('role')=='global-admin': return True
        if ident.get('tenant_id')==target_tenant: return True
        return ident.get('role')=='delegated-admin' and target_tenant in ident.get('delegated_tenants',[])
    def _doc_access(self,tenant,doc_id,ident,owner_required=False):
        doc=DOCS.get(doc_id)
        if doc is None or tenant!=doc['tenant_id']:
            self._json(404,{'error':'document_not_found'}); return None
        if MODE=='secure' and ident.get('tenant_id')!=doc['tenant_id']:
            self._json(403,{'error':'access_denied','tenant_id':doc['tenant_id']}); return None
        if MODE=='secure' and owner_required and ident.get('sub')!=doc['owner_id'] and ident.get('role')!='admin':
            self._json(403,{'error':'access_denied','owner_id':doc['owner_id']}); return None
        return doc
    def do_GET(self):
        parsed=urlparse(self.path); path=parsed.path; query=parse_qs(parsed.query)
        if path=='/health': return self._json(200,{'status':'ok','mode':MODE})
        if path=='/api/v1/s10/session-context':
            ident=self._require_identity()
            if not ident:return
            auth=self.headers.get('Authorization','')
            token=auth[7:].strip()
            scope=claim(token,'scope')
            if isinstance(scope,str):
                scopes=sorted([part for part in scope.split(' ') if part])
            elif isinstance(scope,list):
                scopes=sorted([str(part) for part in scope if str(part)])
            else:
                scopes=[]
            session_id=self.headers.get('X-Session-ID','UNKNOWN')
            return self._json(200,{'session_id':session_id,
                                   'principal_id':ident.get('sub'),
                                   'role':ident.get('role'),
                                   'tenant_id':ident.get('tenant_id'),
                                   'scopes':scopes,
                                   'authentication_type':'OAUTH',
                                   'authorization_mode':MODE})
        if path=='/api/v1/s4/application-denial':
            ident=self._require_identity()
            if not ident:return
            return self._json(200,{'success':False,'message':'Access denied','timestamp':time.time_ns(),'request_id':f's4-deny-{id(self)}'})
        if path=='/api/v1/s4/public':
            variant=query.get('variant',['a'])[0]
            if variant=='b':
                return self._json(200,{'request_id':f's4-public-{id(self)}','timestamp':time.time_ns(),'name':'Public fixture','id':'public-1'},pretty=True)
            return self._json(200,{'id':'public-1','name':'Public fixture','timestamp':time.time_ns(),'request_id':f's4-public-{id(self)}'})
        if path=='/api/v1/s4/slow':
            ident=self._require_identity()
            if not ident:return
            ms=max(0,min(int(query.get('ms',['250'])[0]),2000)); time.sleep(ms/1000.0)
            return self._json(200,{'status':'ok','slept_ms':ms})
        m=S4_DOC_RE.match(path)
        if m:
            ident=self._require_identity()
            if not ident:return
            doc=S4_DOCS.get(m.group('doc'))
            if not doc:return self._json(404,{'error':'document_not_found'})
            mismatch=(ident.get('tenant_id')!=doc['tenant_id'] or ident.get('sub')!=doc['owner_id'])
            if MODE=='secure' and mismatch:
                return self._json(403,{'error':'access_denied','resource_id':doc['id']})
            if MODE=='vulnerable' and mismatch and doc['id']!='Document-B':
                return self._json(403,{'error':'access_denied','resource_id':doc['id']})
            return self._json(200,{**doc,'timestamp':time.time_ns(),'request_id':f's4-doc-{id(self)}'},
                              {'Set-Cookie':'s4session=synthetic-cookie-secret; Path=/; HttpOnly; SameSite=Strict'})
        if path in ('/api/v1/s8/admin','/api//v1/s8/admin'):
            ident=self._require_identity()
            if not ident:return
            equivalent=(path=='/api//v1/s8/admin')
            if ident.get('role')!='admin':
                if MODE=='vulnerable' and equivalent:
                    return self._json(200,{'area':'s8-admin','role':ident.get('role'),
                                           'route_form':'duplicate-separator','authorization_mode':MODE})
                return self._json(403,{'error':'access_denied','required_role':'admin',
                                       'route_form':'duplicate-separator' if equivalent else 'canonical'})
            return self._json(200,{'area':'s8-admin','role':ident.get('role'),
                                   'route_form':'duplicate-separator' if equivalent else 'canonical',
                                   'authorization_mode':MODE})
        m=S9_PROFILE_RE.match(path)
        if m:
            ident=self._require_identity()
            if not ident:return
            profile=S9_PROFILES.get(m.group('user'))
            if profile is None:return self._json(404,{'error':'profile_not_found'})
            if ident.get('sub')!=profile['id']:
                return self._json(403,{'error':'access_denied','reason':'profile_owner_required'})
            visible={'id':profile['id'],'display_name':profile['display_name'],'tenant_id':profile['tenant_id']}
            if MODE=='vulnerable':
                visible['salary_band']=profile['salary_band']
                visible['is_admin']=profile['is_admin']
            return self._json(200,{**visible,'authorization_mode':MODE})

        m=S6_ADMIN_SUMMARY_RE.match(path)
        if m:
            ident=self._require_identity()
            if not ident:return
            tenant=m.group('tenant')
            tenant_allowed=self._s6_tenant_allowed(ident,tenant)
            role_allowed=ident.get('role') in ('admin','tenant-admin','global-admin','delegated-admin')
            if MODE=='secure' and (not tenant_allowed or not role_allowed):
                return self._json(403,{'error':'access_denied','required_role':'tenant-admin'})
            if MODE=='vulnerable' and not tenant_allowed:
                return self._json(403,{'error':'access_denied'})
            return self._json(200,{'tenant_id':tenant,'summary':'privileged-read-only',
                                   'actor':ident.get('sub'),'role':ident.get('role'),
                                   'authorization_mode':MODE})
        m=S6_REPORT_RE.match(path)
        if m:
            ident=self._require_identity()
            if not ident:return
            report=S6_TENANT_REPORTS.get(m.group('tenant'),{}).get(m.group('report'))
            if report is None: report=S6_REPORTS.get(m.group('report'))
            if not report or (report['tenant_id']!='shared' and m.group('tenant')!=report['tenant_id']):
                return self._json(404,{'error':'report_not_found'})
            authorized=self._s6_tenant_allowed(ident,report['tenant_id'])
            if MODE=='secure' and not authorized:
                return self._json(403,{'error':'access_denied','tenant_id':report['tenant_id']})
            if MODE=='vulnerable' and not authorized and report['id'] not in ('report-b','report-common'):
                return self._json(403,{'error':'access_denied','tenant_id':report['tenant_id']})
            return self._json(200,{**report,'authorization_mode':MODE,'request_id':f's6-report-{id(self)}'})
        if path=='/api/v1/search':
            ident=self._require_identity();
            if not ident:return
            tenant=query.get('tenant_id',[ident.get('tenant_id')])[0]
            docs=[d for d in DOCS.values() if d['tenant_id']==tenant]
            if MODE=='secure' and tenant!=ident.get('tenant_id'): return self._json(403,{'error':'access_denied'})
            return self._json(200,{'items':docs,'page':1,'limit':len(docs),'next':None})
        m=DOC_RE.match(path)
        if m:
            ident=self._require_identity();
            if not ident:return
            doc=self._doc_access(m.group('tenant'),m.group('doc'),ident)
            if doc:return self._json(200,{**doc,'api_version':'v'+m.group('version')})
            return
        m=DOC_LIST_RE.match(path)
        if m:
            ident=self._require_identity();
            if not ident:return
            tenant=m.group('tenant')
            if MODE=='secure' and ident.get('tenant_id')!=tenant:return self._json(403,{'error':'access_denied'})
            items=[d for d in DOCS.values() if d['tenant_id']==tenant]
            return self._json(200,{'items':items,'offset':0,'limit':len(items),'request_id':f'list-{id(self)}'})
        m=TENANT_USER_RE.match(path)
        if m:
            ident=self._require_identity();
            if not ident:return
            user=USERS.get(m.group('user'))
            if not user:return self._json(404,{'error':'user_not_found'})
            if MODE=='secure' and (ident.get('tenant_id')!=m.group('tenant') or user['tenant_id']!=m.group('tenant')):return self._json(403,{'error':'access_denied'})
            return self._json(200,user)
        m=USER_RE.match(path)
        if m:
            ident=self._require_identity();
            if not ident:return
            user=USERS.get(m.group('user'))
            if not user:return self._json(404,{'error':'user_not_found'})
            if MODE=='secure' and ident.get('tenant_id')!=user['tenant_id']:return self._json(403,{'error':'access_denied'})
            return self._json(200,user)
        m=COMMENTS_RE.match(path)
        if m:
            ident=self._require_identity();
            if not ident:return
            doc=self._doc_access(m.group('tenant'),m.group('doc'),ident)
            if not doc:return
            return self._json(200,{'items':COMMENTS.get(doc['id'],[]),'document_id':doc['id'],'tenant_id':doc['tenant_id']})
        return self._json(404,{'error':'not_found'})
    def do_PATCH(self):
        parsed=urlparse(self.path)
        m=S9_PROFILE_RE.match(parsed.path)
        if m:
            ident=self._require_identity()
            if not ident:return
            profile=S9_PROFILES.get(m.group('user'))
            if profile is None:return self._json(404,{'error':'profile_not_found'})
            if ident.get('sub')!=profile['id']:
                return self._json(403,{'error':'access_denied','reason':'profile_owner_required'})
            length=min(int(self.headers.get('Content-Length','0') or '0'),65536)
            raw=self.rfile.read(length) if length else b'{}'
            try: body=json.loads(raw.decode())
            except Exception:return self._json(400,{'error':'malformed_json'})
            if not isinstance(body,dict):return self._json(400,{'error':'malformed_profile_update'})
            permitted={'display_name'}
            forbidden=sorted(set(body.keys())-permitted)
            if MODE=='secure' and forbidden:
                return self._json(403,{'error':'property_access_denied','properties':forbidden})
            applied=[]
            if isinstance(body.get('display_name'),str):
                profile['display_name']=body['display_name'];applied.append('display_name')
            if MODE=='vulnerable' and isinstance(body.get('is_admin'),bool):
                profile['is_admin']=body['is_admin'];applied.append('is_admin')
            return self._json(200,{'id':profile['id'],'display_name':profile['display_name'],
                                   'is_admin':profile['is_admin'],'applied_properties':sorted(applied),
                                   'authorization_mode':MODE})
        m=DOC_RE.match(parsed.path)
        if not m:return self._json(404,{'error':'not_found'})
        ident=self._require_identity();
        if not ident:return
        doc=self._doc_access(m.group('tenant'),m.group('doc'),ident,owner_required=True)
        if not doc:return
        length=min(int(self.headers.get('Content-Length','0') or '0'),65536)
        raw=self.rfile.read(length) if length else b'{}'
        try: body=json.loads(raw.decode())
        except Exception:return self._json(400,{'error':'malformed_json'})
        if isinstance(body,dict) and isinstance(body.get('title'),str):doc['title']=body['title']
        return self._json(200,doc)
    def do_POST(self):
        parsed=urlparse(self.path)
        if parsed.path=='/api/v1/s4/echo':
            ident=self._require_identity()
            if not ident:return
            length=min(int(self.headers.get('Content-Length','0') or '0'),65536)
            raw=self.rfile.read(length) if length else b''
            return self._json(200,{'method':'POST','x_echo':self.headers.get('X-Echo',''),'body':raw.decode(errors='replace')})
        m=S7_WORKFLOW_TRANSITION_RE.match(parsed.path)
        if m:
            ident=self._require_identity()
            if not ident:return
            length=min(int(self.headers.get('Content-Length','0') or '0'),65536)
            raw=self.rfile.read(length) if length else b'{}'
            try: body=json.loads(raw.decode())
            except Exception:return self._json(400,{'error':'malformed_json'})
            if not isinstance(body,dict):return self._json(400,{'error':'malformed_workflow_transition'})
            action=str(body.get('action',''))
            from_state=str(body.get('from_state',''))
            to_state=str(body.get('to_state',''))
            approval=bool(body.get('approval',False))
            role_separation=bool(body.get('role_separation',False))
            workflow=m.group('workflow'); resource=m.group('resource')
            allowed=False
            reason='transition_denied'
            if workflow!='document-approval':
                reason='unknown_workflow'
            elif action=='SUBMIT' and from_state=='DRAFT':
                if ident.get('role')!='author':
                    reason='required_role_author'
                elif MODE=='vulnerable':
                    allowed=True
                elif to_state=='SUBMITTED':
                    allowed=True
                else:
                    reason='invalid_target_state'
            elif action=='APPROVE' and from_state=='SUBMITTED' and to_state=='APPROVED':
                if ident.get('role')!='approver': reason='required_role_approver'
                elif not approval: reason='approval_required'
                elif not role_separation: reason='role_separation_required'
                else: allowed=True
            elif from_state=='SHIPPED':
                reason='terminal_state'
            else:
                reason='transition_rule_missing'
            if not allowed:return self._json(403,{'error':'access_denied','reason':reason,'workflow':workflow})
            return self._json(200,{'workflow':workflow,'resource_id':resource,'action':action,
                                   'from_state':from_state,'to_state':to_state,'state':to_state,
                                   'actor':ident.get('sub'),'role':ident.get('role'),
                                   'authorization_mode':MODE,'persisted':False})
        m=S6_EXPORT_RE.match(parsed.path)
        if m:
            ident=self._require_identity()
            if not ident:return
            tenant=m.group('tenant')
            tenant_allowed=self._s6_tenant_allowed(ident,tenant)
            role_allowed=ident.get('role') in ('admin','tenant-admin','global-admin','delegated-admin')
            if MODE=='secure' and (not tenant_allowed or not role_allowed):
                return self._json(403,{'error':'access_denied','required_role':'tenant-admin'})
            if MODE=='vulnerable' and not tenant_allowed and ident.get('sub')!='user-a':
                return self._json(403,{'error':'access_denied'})
            return self._json(200,{'export':'accepted','tenant_id':tenant,'actor':ident.get('sub'),
                                   'role':ident.get('role'),'authorization_mode':MODE})
        m=APPROVE_RE.match(parsed.path)
        if not m:return self._json(404,{'error':'not_found'})
        ident=self._require_identity();
        if not ident:return
        doc=self._doc_access(m.group('tenant'),m.group('doc'),ident)
        if not doc:return
        if MODE=='secure' and ident.get('role') not in ('editor','admin'):return self._json(403,{'error':'access_denied','required_role':'editor'})
        doc['state']='approved';return self._json(200,doc)

if __name__=='__main__':
    ThreadingHTTPServer(('127.0.0.1',PORT),Handler).serve_forever()
