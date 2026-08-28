(function(){
'use strict';
const RV='0.8.2';
const CV='1.0';
const AudioCtx=window.AudioContext||window.webkitAudioContext;
const status=document.getElementById('sketsa-audio-runtime-status');
if(!AudioCtx){if(status)status.textContent='Web Audio API unavailable';return;}
const ctx=new AudioCtx();
const entries=[];const byId=new Map();let voiceSerial=0;
const master={volume:1,mute:false,gain:ctx.createGain()};
master.gain.connect(ctx.destination);
const buses=new Map();
function num(v,d){const n=Number(v);return Number.isFinite(n)?n:d;}
function clamp(v,a,b){return Math.max(a,Math.min(b,v));}
function normBus(v){v=String(v||'').trim().replace(/[^A-Za-z0-9_-]+/g,'-').replace(/^-+|-+$/g,'');return v||'main';}
function applyMaster(){master.gain.gain.setValueAtTime(master.mute?0:master.volume,ctx.currentTime);}
function getOrCreateBus(name){name=normBus(name);let b=buses.get(name);if(!b){b={id:name,volume:1,mute:false,gain:ctx.createGain()};b.gain.connect(master.gain);b.gain.gain.value=1;buses.set(name,b);}return b;}
function applyBus(b){b.gain.gain.setValueAtTime(b.mute?0:b.volume,ctx.currentTime);}
function applySource(e){e.gain.gain.setValueAtTime(e.mute?0:e.volume,ctx.currentTime);}
function applyPan(e){if(e.panner&&e.panner.pan)e.panner.pan.setValueAtTime(clamp(e.pan,-1,1),ctx.currentTime);}
function svgPanFor(e){try{const root=e.el.ownerSVGElement||document.querySelector('svg');if(!root)return e.pan;let minX=0,width=0;const vb=root.viewBox&&root.viewBox.baseVal;if(vb&&vb.width>0){minX=vb.x;width=vb.width;}else{width=num(root.getAttribute('width'),0);if(!(width>0)){const rr=root.getBoundingClientRect();width=rr.width;}}if(!(width>0))return e.pan;const box=e.el.getBBox();const ctm=e.el.getCTM();let cx=box.x+box.width/2;if(ctm){cx=ctm.a*cx+ctm.c*(box.y+box.height/2)+ctm.e;}return clamp(((cx-minX)/width)*2-1,-1,1);}catch(_){return e.pan;}}
function refreshAutoPan(e){if(!e||e.panMode!=='svg-x')return false;e.pan=svgPanFor(e);applyPan(e);return true;}
function refreshAllAutoPan(){entries.forEach(refreshAutoPan);return true;}
function loadMixMetadata(){const root=document.querySelector('svg');if(!root)return;master.volume=clamp(num(root.getAttribute('data-sketsa-audio-master-volume'),1),0,4);master.mute=root.getAttribute('data-sketsa-audio-master-mute')==='true';Array.from(root.attributes||[]).forEach(a=>{let m=a.name.match(/^data-sketsa-audio-bus-(.+)-(volume|mute)$/);if(!m)return;const b=getOrCreateBus(m[1]);if(m[2]==='volume')b.volume=clamp(num(a.value,1),0,4);else b.mute=a.value==='true';});applyMaster();buses.forEach(applyBus);}
function dataBytes(uri){const c=uri.indexOf(',');const h=uri.slice(0,c),b=uri.slice(c+1);if(/;base64/i.test(h)){const s=atob(b),u=new Uint8Array(s.length);for(let i=0;i<s.length;i++)u[i]=s.charCodeAt(i);return u.buffer;}const s=decodeURIComponent(b),u=new Uint8Array(s.length);for(let i=0;i<s.length;i++)u[i]=s.charCodeAt(i)&255;return u.buffer;}
async function bytes(src){const fallback=(window.__SketsaAudioAssetData||{})[src];if(location.protocol==='file:'&&fallback)return dataBytes(fallback);try{const r=await fetch(src);if(!r.ok)throw new Error('HTTP '+r.status+' '+src);return await r.arrayBuffer();}catch(err){if(fallback)return dataBytes(fallback);throw err;}}
function emit(name,type,e,extra){const detail={contractVersion:CV,source:'audio',type:type,data:Object.assign({id:e&&e.id||'',eventId:e&&e.eventId||''},extra||{})};document.dispatchEvent(new CustomEvent(name,{detail:detail}));document.dispatchEvent(new CustomEvent('sketsa:audio:event',{detail:detail}));document.dispatchEvent(new CustomEvent('sketsa:runtime:event',{detail:detail}));return detail;}
function emitReady(){const detail={contractVersion:CV,source:'audio',type:'ready',runtimeVersion:RV,data:{sources:listSources(),buses:listBuses(),master:getMaster()}};document.dispatchEvent(new CustomEvent('sketsa:audio:ready',{detail:detail}));document.dispatchEvent(new CustomEvent('sketsa:audio:event',{detail:detail}));document.dispatchEvent(new CustomEvent('sketsa:runtime:event',{detail:detail}));return detail;}
function normalizeOffset(e,v){const d=e.buffer?e.buffer.duration:0;let o=Math.max(0,num(v,0));if(d>0){if(e.loop)o=o%d;else o=Math.min(o,Math.max(0,d-0.000001));}return o;}
function state(e){return{id:e.id,svgId:e.svgId,eventId:e.eventId,src:e.src,assetKey:e.assetKey,missing:e.missing,loaded:!!e.buffer,status:e.status,playing:e.voices.size>0,activeVoices:e.voices.size,loop:e.loop,volume:e.volume,mute:e.mute,bus:e.bus.id,pan:e.pan,panMode:e.panMode,playbackRate:e.playbackRate,startOffset:e.startOffset,pausedOffset:e.pausedOffset,duration:e.buffer?e.buffer.duration:0,triggerSource:e.triggerSource,triggerType:e.triggerType,triggerEventId:e.triggerEventId,triggerAction:e.triggerAction};}
function busState(b){return{id:b.id,volume:b.volume,mute:b.mute};}
function masterState(){return{volume:master.volume,mute:master.mute};}

async function load(el,i){
 const id=el.getAttribute('data-sketsa-audio-runtime-id')||el.getAttribute('id')||('audio-'+(i+1));
 const svgId=el.getAttribute('data-sketsa-audio-svg-id')||el.getAttribute('id')||'';
 const src=el.getAttribute('data-sketsa-audio-runtime-src')||el.getAttribute('data-sketsa-audio-src');
 const missing=el.getAttribute('data-sketsa-audio-missing')==='true';
 const b=getOrCreateBus(el.getAttribute('data-sketsa-audio-bus')||'main');
 const e={el:el,id:id,svgId:svgId,eventId:el.getAttribute('data-sketsa-audio-event-id')||id,src:src,assetKey:el.getAttribute('data-sketsa-audio-asset-key')||'',missing:missing,
   loop:el.getAttribute('data-sketsa-audio-loop')==='true',autoplay:el.getAttribute('data-sketsa-audio-autoplay')==='true',
   volume:clamp(num(el.getAttribute('data-sketsa-audio-volume'),1),0,4),mute:el.getAttribute('data-sketsa-audio-mute')==='true',bus:b,
   playbackRate:clamp(num(el.getAttribute('data-sketsa-audio-playback-rate'),1),0.05,8),
   pan:clamp(num(el.getAttribute('data-sketsa-audio-pan'),0),-1,1),panMode:el.getAttribute('data-sketsa-audio-pan-mode')==='svg-x'?'svg-x':'manual',
   startOffset:Math.max(0,num(el.getAttribute('data-sketsa-audio-start-offset'),0)),triggerSource:String(el.getAttribute('data-sketsa-audio-trigger-source')||'').trim(),triggerType:String(el.getAttribute('data-sketsa-audio-trigger-type')||'').trim(),triggerEventId:String(el.getAttribute('data-sketsa-audio-trigger-event-id')||'').trim(),triggerAction:String(el.getAttribute('data-sketsa-audio-trigger-action')||'play').trim()||'play',pausedOffset:null,buffer:null,gain:ctx.createGain(),panner:ctx.createStereoPanner?ctx.createStereoPanner():ctx.createGain(),voices:new Map(),primaryVoice:null,status:missing?'error':'loading'};
 applySource(e);if(e.panMode==='svg-x')e.pan=svgPanFor(e);applyPan(e);e.gain.connect(e.panner);e.panner.connect(b.gain);entries.push(e);byId.set(id,e);
 if(missing){emit('sketsa:audio:error','error',e,{message:'Audio asset missing at export',missing:true});return e;}
 try{const raw=await bytes(src);e.buffer=await ctx.decodeAudioData(raw.slice(0));e.startOffset=normalizeOffset(e,e.startOffset);e.status='loaded';emit('sketsa:audio:loaded','loaded',e,{duration:e.buffer.duration});return e;}catch(err){e.status='error';e.missing=true;emit('sketsa:audio:error','error',e,{message:String(err&&err.message||err),missing:true});return e;}
}
function stopVoice(e,v,reason){if(!v)return;v.reason=reason||'stop';try{v.node.onended=null;v.node.stop();}catch(_){}e.voices.delete(v.voiceId);if(e.primaryVoice===v)e.primaryVoice=null;}
function stopAll(e,reason){Array.from(e.voices.values()).forEach(v=>stopVoice(e,v,reason));}
function makeVoice(e,offset,isPrimary){const n=ctx.createBufferSource();n.buffer=e.buffer;n.loop=e.loop;n.playbackRate.value=e.playbackRate;n.connect(e.gain);const voice={voiceId:++voiceSerial,node:n,offset:normalizeOffset(e,offset),startedAt:ctx.currentTime,rate:e.playbackRate,isPrimary:!!isPrimary,reason:null};e.voices.set(voice.voiceId,voice);if(isPrimary)e.primaryVoice=voice;n.onended=function(){if(!e.voices.has(voice.voiceId))return;e.voices.delete(voice.voiceId);if(e.primaryVoice===voice)e.primaryVoice=null;if(e.voices.size===0)e.status='ended';emit('sketsa:audio:ended','ended',e,{voiceId:voice.voiceId});};n.start(0,voice.offset);return voice;}
function getEntry(id){return byId.get(String(id));}
function play(id,options){const e=getEntry(id);if(!e||!e.buffer)return false;options=options||{};const overlap=options.overlap===true;if(!overlap&&e.primaryVoice)stopVoice(e,e.primaryVoice,'replace');const off=options.offset!=null?options.offset:(e.pausedOffset!=null?e.pausedOffset:e.startOffset);const v=makeVoice(e,off,!overlap);e.pausedOffset=null;e.status='playing';emit('sketsa:audio:play','play',e,{voiceId:v.voiceId,overlap:overlap,offset:v.offset});return true;}
function pause(id){const e=getEntry(id);if(!e||!e.primaryVoice)return false;const v=e.primaryVoice;const elapsed=Math.max(0,ctx.currentTime-v.startedAt)*v.rate;e.pausedOffset=normalizeOffset(e,v.offset+elapsed);stopVoice(e,v,'pause');e.status=e.voices.size>0?'playing':'paused';emit('sketsa:audio:pause','pause',e,{offset:e.pausedOffset});return true;}
function stop(id){const e=getEntry(id);if(!e)return false;stopAll(e,'stop');e.pausedOffset=null;e.status='stopped';emit('sketsa:audio:stop','stop',e,{});return true;}
function restart(id){const e=getEntry(id);if(!e||!e.buffer)return false;stopAll(e,'restart');e.pausedOffset=null;e.status='stopped';return play(id,{offset:e.startOffset});}
function setVolume(id,value){const e=getEntry(id);if(!e)return false;e.volume=clamp(num(value,e.volume),0,4);applySource(e);emit('sketsa:audio:mix','mix',e,{scope:'source',volume:e.volume,mute:e.mute,bus:e.bus.id});return true;}
function setMute(id,value){const e=getEntry(id);if(!e)return false;e.mute=!!value;applySource(e);emit('sketsa:audio:mix','mix',e,{scope:'source',volume:e.volume,mute:e.mute,bus:e.bus.id});return true;}
function setPlaybackRate(id,value){const e=getEntry(id);if(!e)return false;e.playbackRate=clamp(num(value,e.playbackRate),0.05,8);e.voices.forEach(v=>{v.node.playbackRate.setValueAtTime(e.playbackRate,ctx.currentTime);v.rate=e.playbackRate;});return true;}
function setLoop(id,value){const e=getEntry(id);if(!e)return false;e.loop=!!value;e.voices.forEach(v=>{v.node.loop=e.loop;});return true;}
function setPan(id,value){const e=getEntry(id);if(!e)return false;e.panMode='manual';e.pan=clamp(num(value,e.pan),-1,1);applyPan(e);emit('sketsa:audio:spatial','spatial',e,{pan:e.pan,panMode:e.panMode});return true;}
function setPanMode(id,mode){const e=getEntry(id);if(!e)return false;e.panMode=String(mode)==='svg-x'?'svg-x':'manual';if(e.panMode==='svg-x'){refreshAutoPan(e);ensureSpatialTick();}else applyPan(e);emit('sketsa:audio:spatial','spatial',e,{pan:e.pan,panMode:e.panMode});return true;}
function updateAutoPan(id){const e=getEntry(id);return !!e&&refreshAutoPan(e);}
function setBus(id,name){const e=getEntry(id);if(!e)return false;const b=getOrCreateBus(name);try{e.panner.disconnect();}catch(_){}e.panner.connect(b.gain);e.bus=b;emit('sketsa:audio:mix','mix',e,{scope:'routing',bus:b.id});return true;}
function setBusVolume(name,value){const b=getOrCreateBus(name);b.volume=clamp(num(value,b.volume),0,4);applyBus(b);emit('sketsa:audio:mix','mix',null,{scope:'bus',bus:b.id,volume:b.volume,mute:b.mute});return true;}
function setBusMute(name,value){const b=getOrCreateBus(name);b.mute=!!value;applyBus(b);emit('sketsa:audio:mix','mix',null,{scope:'bus',bus:b.id,volume:b.volume,mute:b.mute});return true;}
function setMasterVolume(value){master.volume=clamp(num(value,master.volume),0,4);applyMaster();emit('sketsa:audio:mix','mix',null,{scope:'master',volume:master.volume,mute:master.mute});return true;}
function setMasterMute(value){master.mute=!!value;applyMaster();emit('sketsa:audio:mix','mix',null,{scope:'master',volume:master.volume,mute:master.mute});return true;}
function getSource(id){const e=getEntry(id);return e?state(e):null;}
function listSources(){return entries.map(state);}
function getBus(name){const b=buses.get(normBus(name));return b?busState(b):null;}
function listBuses(){return Array.from(buses.values()).map(busState);}
function getMaster(){return masterState();}
async function resume(){await ctx.resume();return ctx.state;}

loadMixMetadata();
function actionValue(d){return Object.prototype.hasOwnProperty.call(d,'value')?d.value:(d.data&&Object.prototype.hasOwnProperty.call(d.data,'value')?d.data.value:undefined);}
function actionTarget(d){return String(d.target||d.id||(d.data&&(d.data.target||d.data.id))||'');}
async function executeAction(d){d=d||{};const a=String(d.action||d.type||'');const target=actionTarget(d),v=actionValue(d),o=d.options||(d.data&&d.data.options)||{};let ok=false,result=null;
 switch(a){
  case 'play': ok=play(target,o);break;case 'pause':ok=pause(target);break;case 'stop':ok=stop(target);break;case 'restart':ok=restart(target);break;
  case 'setVolume':ok=setVolume(target,v);break;case 'setMute':ok=setMute(target,v);break;case 'setPlaybackRate':ok=setPlaybackRate(target,v);break;case 'setLoop':ok=setLoop(target,v);break;
  case 'setPan':ok=setPan(target,v);break;case 'setPanMode':ok=setPanMode(target,v);break;case 'setBus':ok=setBus(target,v);break;
  case 'setBusVolume':ok=setBusVolume(target||d.bus,v);break;case 'setBusMute':ok=setBusMute(target||d.bus,v);break;
  case 'setMasterVolume':ok=setMasterVolume(v);break;case 'setMasterMute':ok=setMasterMute(v);break;
  case 'resume':result=await resume();ok=true;break;default:throw new Error('Unknown audio action: '+a);
 }
 if(result==null){if(target&&getSource(target))result=getSource(target);else if(a.indexOf('Master')>=0)result=getMaster();else if((a==='setBusVolume'||a==='setBusMute')&&(target||d.bus))result=getBus(target||d.bus);else result={ok:!!ok};}
 return{ok:!!ok,action:a,target:target,data:result};}
function actionResult(detail,ok,payload,error){const out={contractVersion:CV,source:'audio',type:'actionResult',requestId:String(detail&&detail.requestId||''),action:String(detail&&detail.action||detail&&detail.type||''),target:actionTarget(detail||{}),ok:!!ok,data:payload||null,error:error?String(error):''};document.dispatchEvent(new CustomEvent('sketsa:audio:actionResult',{detail:out}));return out;}
document.addEventListener('sketsa:audio:action',async function(ev){const d=ev.detail||{};try{const r=await executeAction(d);actionResult(d,r.ok,r.data,r.ok?'':'Action returned false');}catch(err){actionResult(d,false,null,err&&err.message||err);}});
let interopSerial=0;
function interopMatch(e,d){if(!e||!d||!e.triggerSource||!e.triggerType)return false;if(String(d.source||'')!==e.triggerSource||String(d.type||'')!==e.triggerType)return false;const data=d.data||{};return !e.triggerEventId||String(data.eventId||'')===e.triggerEventId;}
function publishInteropAction(e,d){const detail={contractVersion:CV,requestId:'interop-'+Date.now()+'-'+(++interopSerial),action:e.triggerAction||'play',target:e.id,options:{overlap:true},origin:{source:String(d.source||''),type:String(d.type||''),eventId:String((d.data&&d.data.eventId)||'')}};document.dispatchEvent(new CustomEvent('sketsa:audio:action',{detail:detail}));return detail;}
document.addEventListener('sketsa:runtime:event',function(ev){const d=ev.detail||{};if(d.source==='audio')return;entries.forEach(e=>{if(interopMatch(e,d))publishInteropAction(e,d);});});
let requestSerial=0;function requestAction(action,target,value,options){return new Promise(resolve=>{const requestId='audio-'+Date.now()+'-'+(++requestSerial);const on=function(ev){if(ev.detail&&ev.detail.requestId===requestId){document.removeEventListener('sketsa:audio:actionResult',on);resolve(ev.detail);}};document.addEventListener('sketsa:audio:actionResult',on);document.dispatchEvent(new CustomEvent('sketsa:audio:action',{detail:{contractVersion:CV,requestId:requestId,action:action,target:target||'',value:value,options:options||{}}}));});}
function snapshot(){return{contractVersion:CV,runtimeVersion:RV,sources:listSources(),buses:listBuses(),master:getMaster()};}
window.SketsaAudio={contractVersion:CV,runtimeVersion:RV,context:ctx,play:play,pause:pause,stop:stop,restart:restart,setVolume:setVolume,setMute:setMute,setPlaybackRate:setPlaybackRate,setLoop:setLoop,setPan:setPan,setPanMode:setPanMode,updateAutoPan:updateAutoPan,updateAllAutoPan:refreshAllAutoPan,setBus:setBus,setBusVolume:setBusVolume,setBusMute:setBusMute,setMasterVolume:setMasterVolume,setMasterMute:setMasterMute,getSource:getSource,listSources:listSources,getBus:getBus,listBuses:listBuses,getMaster:getMaster,snapshot:snapshot,requestAction:requestAction,resume:resume};
const enable=document.getElementById('sketsa-audio-enable');if(enable)enable.addEventListener('click',async function(){await resume();entries.forEach(e=>{if(e.autoplay&&!e.voices.size)play(e.id);});if(status)status.textContent='Audio enabled';});
let spatialFrame=0;function spatialTick(){let any=false;entries.forEach(e=>{if(e.panMode==='svg-x'){any=true;refreshAutoPan(e);}});if(any)spatialFrame=requestAnimationFrame(spatialTick);else spatialFrame=0;}function ensureSpatialTick(){if(!spatialFrame&&entries.some(e=>e.panMode==='svg-x'))spatialFrame=requestAnimationFrame(spatialTick);}

const a8Root=document.querySelector('svg[data-sketsa-audio-autotest="a8"]');
const a8Seen={ready:false,errors:[],physics:[],actions:[],results:[],plays:[],neutral:[]};
if(a8Root){
 document.addEventListener('sketsa:audio:ready',ev=>{if(ev.detail&&ev.detail.source==='audio')a8Seen.ready=true;});
 document.addEventListener('sketsa:audio:error',ev=>{if(ev.detail)a8Seen.errors.push(ev.detail);});
 document.addEventListener('sketsa:runtime:event',ev=>{if(ev.detail&&ev.detail.source==='physics')a8Seen.physics.push(ev.detail);if(ev.detail&&ev.detail.source==='audio')a8Seen.neutral.push(ev.detail);});
 document.addEventListener('sketsa:audio:action',ev=>{if(ev.detail&&String(ev.detail.requestId||'').indexOf('interop-')===0)a8Seen.actions.push(ev.detail);});
 document.addEventListener('sketsa:audio:actionResult',ev=>{if(ev.detail&&String(ev.detail.requestId||'').indexOf('interop-')===0)a8Seen.results.push(ev.detail);});
 document.addEventListener('sketsa:audio:play',ev=>{if(ev.detail)a8Seen.plays.push(ev.detail);});
}
function plainSnapshotOk(v){try{const t=JSON.stringify(v);return !!t&&!/(gain|panner|buffer|voices|primaryVoice|AudioContext|AudioBuffer)/.test(t);}catch(_){return false;}}
async function a8Autotest(){
 if(!a8Root)return false;
 const api=!!window.SketsaAudio&&window.SketsaAudio.runtimeVersion===RV&&window.SketsaAudio.contractVersion===CV&&['snapshot','requestAction','getSource','listSources','play','stop'].every(k=>typeof window.SketsaAudio[k]==='function');
 const sources=listSources();
 const dup1=getSource('a8-dup'),dup2=getSource('a8-dup~2');
 const duplicate=!!dup1&&!!dup2&&dup1.svgId==='a8-dup'&&dup2.svgId==='a8-dup'&&dup1.id!==dup2.id&&dup1.assetKey&&dup1.assetKey===dup2.assetKey;
 const noId=sources.find(x=>x.svgId==='');const idless=!!noId&&/^audio-\d+(?:~\d+)?$/.test(noId.id)&&noId.loaded;
 const missing=sources.find(x=>x.eventId==='missing-asset');const missingOk=!!missing&&missing.missing===true&&missing.loaded===false&&missing.status==='error'&&a8Seen.errors.some(e=>e.data&&e.data.id===missing.id&&e.data.missing===true);
 const binding=!!dup1&&dup1.triggerSource==='physics'&&dup1.triggerType==='collisionStart'&&dup1.triggerEventId==='impact'&&dup1.triggerAction==='play';
 document.dispatchEvent(new CustomEvent('sketsa:runtime:event',{detail:{contractVersion:'1.0',source:'physics',type:'collisionStart',data:{eventId:'impact',selfId:'a8-ball',otherId:'a8-floor'}}}));
 await new Promise(r=>setTimeout(r,120));
 const interop=a8Seen.actions.some(a=>a.target==='a8-dup')&&a8Seen.results.some(r=>r.ok===true&&r.target==='a8-dup')&&a8Seen.plays.some(e=>e.data&&e.data.id==='a8-dup');
 const metadata=Array.from(a8Root.querySelectorAll('[data-sketsa-audio-src]')).every(el=>el.hasAttribute('data-sketsa-audio-runtime-id'))&&dup1.bus==='sfx'&&Math.abs(dup1.volume-0.5)<0.001;
 const shared=sources.filter(x=>x.assetKey===dup1.assetKey&&x.loaded).length>=3;
 const snapshot=plainSnapshotOk(window.SketsaAudio.snapshot())&&sources.every(plainSnapshotOk);
 const regression=!!getBus('sfx')&&Math.abs(getMaster().volume-0.9)<0.001&&typeof setPanMode==='function';
 const local=Array.from(document.scripts).some(x=>/\/sketsa-audio-runtime\.js(?:\?|$)/.test(x.getAttribute('src')||''));
 const metaEl=document.querySelector('meta[name="sketsa-audio-runtime"]');const meta=!!metaEl&&metaEl.content.indexOf(RV)===0;
 const ok=a8Seen.ready&&api&&duplicate&&idless&&missingOk&&binding&&interop&&metadata&&shared&&snapshot&&regression&&local&&meta;
 const text=(ok?'AUTOTEST PASS':'AUTOTEST FAIL')+': ready='+a8Seen.ready+', api='+api+', duplicate='+duplicate+', idless='+idless+', missing='+missingOk+', binding='+binding+', interop='+interop+', metadata='+metadata+', shared='+shared+', snapshot='+snapshot+', regression='+regression+', local='+local+', meta='+meta;
 if(status)status.textContent=text;const out=document.getElementById('audio-a8-status');if(out)out.textContent=text;return true;
}

Promise.all(Array.from(document.querySelectorAll('[data-sketsa-audio-src]')).map(load)).then(async function(){
 refreshAllAutoPan();ensureSpatialTick();emitReady();
 const handled=await a8Autotest();
 if(!handled&&status){const meta=document.querySelector('meta[name="sketsa-audio-runtime"]');const metaOk=!!meta&&meta.content.indexOf(RV)===0,apiOk=!!window.SketsaAudio&&window.SketsaAudio.runtimeVersion===RV,loaded=entries.length>0&&entries.every(e=>!!e.buffer);status.textContent=(metaOk&&apiOk&&loaded?'Audio ready':'Audio load failed')+': api='+apiOk+', loaded='+loaded+', meta='+metaOk;}
}).catch(function(err){if(status)status.textContent='AUTOTEST FAIL: '+err.message;const out=document.getElementById('audio-a8-status');if(out)out.textContent='AUTOTEST FAIL: '+err.message;console.error(err);});
})();
