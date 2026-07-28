---- MODULE RaftNodeSpec ----
EXTENDS Integers, Sequences, FiniteSets, TLC

CONSTANT String

VARIABLES RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException

vars == <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

CanStartSession_ElectionTimeout_TimerController(n, n_TimerController) ==
  /\ ~\E n_TimerController2 \in Int : session_ElectionTimeout_TimerController[n][n_TimerController2]
  /\ ~\E n2 \in Int : session_ElectionTimeout_TimerController[n2][n_TimerController]

CanStartSession_TimerController_TimerHelper(n, n_TimerHelper) ==
  /\ ~\E n_TimerHelper2 \in Int : session_TimerController_TimerHelper[n][n_TimerHelper2]
  /\ ~\E n2 \in Int : session_TimerController_TimerHelper[n2][n_TimerHelper]

\* True when all of ElectionTimeout's actions are disabled.
ElectionTimeout_dead(n) ==
  /\ ElectionTimeout_constructed[n]
  /\ ~((ElectionTimeout_step[n] = "createTimer"))
  /\ ~(\E time \in Int : (((ElectionTimeout_step[n] = "startTimer") \/ (ElectionTimeout_step[n] = "restart")) /\ (time = electTimeout[n])))
  /\ ~((ElectionTimeout_step[n] = "wait"))
  /\ ~((ElectionTimeout_step[n] = "wait"))
  /\ ~((ElectionTimeout_step[n] = "cancel"))
  /\ ~((ElectionTimeout_step[n] = "doTimeout"))

\* True when all of TimerController's actions are disabled.
TimerController_dead(n) ==
  /\ TimerController_constructed[n]
  /\ ~(~(timing[n]))
  /\ ~(ringTimeout[n])
  /\ ~(timing[n])
  /\ ~(\E id \in Int : \E time \in Int : (((~(timing[n]) /\ start[n]) /\ (id = currentId[n])) /\ (time = timeDuration[n])))
  /\ ~(timing[n])

\* True when TimerHelper was explicitly killed or all of its actions are disabled.
TimerHelper_dead(n) ==
  \/ TimerHelper_killed[n]
  \/ (/\ TimerHelper_constructed[n]
       /\ ~(\E id \in Int : (~(TimerHelper_done[n]) /\ (id = timerId[n]))))

Init ==
  \* State variables for RaftNodeMain
  /\ RaftNodeMain_constructed = [n \in Int |-> FALSE]
  /\ RaftNodeMain_self = [n \in Int |-> [id |-> 0, url |-> ""]]
  /\ theCluster = [n \in Int |-> <<>>]
  /\ RaftNodeMain_listenPort = [n \in Int |-> 0]
  /\ RaftNodeMain_step = [n \in Int |-> ""]
  \* State variables for RaftCore
  /\ RaftCore_constructed = [n \in Int |-> FALSE]
  /\ RaftCore_self = [n \in Int |-> [id |-> 0, url |-> ""]]
  /\ selfId = [n \in Int |-> 0]
  /\ RaftCore_cluster = [n \in Int |-> <<>>]
  /\ clusterSize = [n \in Int |-> 0]
  /\ currentTerm = [n \in Int |-> 0]
  /\ state = [n \in Int |-> ""]
  /\ votedFor = [n \in Int |-> 0]
  /\ log = [n \in Int |-> <<>>]
  /\ lastLogTermVar = [n \in Int |-> 0]
  /\ commitIndex = [n \in Int |-> 0]
  /\ lastApplied = [n \in Int |-> 0]
  /\ sm = [n \in Int |-> <<>>]
  /\ votesGranted = [n \in Int |-> {}]
  /\ voteCount = [n \in Int |-> 0]
  /\ knownLeaderId = [n \in Int |-> 0]
  /\ nextIndex = [n \in Int |-> [x \in {} |-> 0]]
  /\ matchIndex = [n \in Int |-> [x \in {} |-> 0]]
  /\ ackCount = [n \in Int |-> 0]
  /\ spawnElectionTimeout = [n \in Int |-> FALSE]
  /\ spawnElection = [n \in Int |-> FALSE]
  /\ spawnLeaderHeartbeat = [n \in Int |-> FALSE]
  /\ resetElectionTimer = [n \in Int |-> FALSE]
  /\ aePeer = [n \in Int |-> [id |-> 0, url |-> ""]]
  /\ aeTerm = [n \in Int |-> 0]
  /\ aeLeaderId = [n \in Int |-> 0]
  /\ aePrevIdx = [n \in Int |-> 0]
  /\ aePrevTerm = [n \in Int |-> 0]
  /\ aeEntryTerm = [n \in Int |-> 0]
  /\ aeEntryValue = [n \in Int |-> ""]
  /\ aeEntriesLen = [n \in Int |-> 0]
  /\ aeLeaderCommit = [n \in Int |-> 0]
  /\ aeReady = [n \in Int |-> FALSE]
  \* State variables for RpcReqHandler
  /\ RpcReqHandler_constructed = [n \in Int |-> FALSE]
  /\ RpcReqHandler_httpResp = [n \in Int |-> [body |-> "", code |-> 0]]
  /\ RpcReqHandler_done = [n \in Int |-> FALSE]
  \* State variables for ServerInitializer
  /\ ServerInitializer_constructed = [n \in Int |-> FALSE]
  /\ started = [n \in Int |-> FALSE]
  /\ ServerInitializer_listenPort = [n \in Int |-> 0]
  \* State variables for RpcOutClient
  /\ RpcOutClient_constructed = [n \in Int |-> FALSE]
  /\ RpcOutClient_step = [n \in Int |-> ""]
  /\ httpReq = [n \in Int |-> [url |-> "", method |-> "", body |-> ""]]
  /\ RpcOutClient_httpResp = [n \in Int |-> [body |-> "", code |-> 0]]
  \* State variables for ElectionTimeout
  /\ ElectionTimeout_constructed = [n \in Int |-> FALSE]
  /\ ElectionTimeout_selfNode = [n \in Int |-> [id |-> 0, url |-> ""]]
  /\ electTimeout = [n \in Int |-> 0]
  /\ ElectionTimeout_step = [n \in Int |-> ""]
  \* State variables for TimerController
  /\ TimerController_constructed = [n \in Int |-> FALSE]
  /\ LARGE_WRAP_AROUND = [n \in Int |-> 0]
  /\ currentId = [n \in Int |-> 0]
  /\ timing = [n \in Int |-> FALSE]
  /\ start = [n \in Int |-> FALSE]
  /\ timeDuration = [n \in Int |-> 0]
  /\ ringTimeout = [n \in Int |-> FALSE]
  \* State variables for TimerHelper
  /\ TimerHelper_constructed = [n \in Int |-> FALSE]
  /\ TimerHelper_killed = [n \in Int |-> FALSE]
  /\ timerId = [n \in Int |-> 0]
  /\ TimerHelper_done = [n \in Int |-> FALSE]
  \* State variables for CandidateElection
  /\ CandidateElection_constructed = [n \in Int |-> FALSE]
  /\ CandidateElection_selfNode = [n \in Int |-> [id |-> 0, url |-> ""]]
  /\ snapTerm = [n \in Int |-> 0]
  /\ snapCand = [n \in Int |-> 0]
  /\ snapLastIdx = [n \in Int |-> 0]
  /\ snapLastTerm = [n \in Int |-> 0]
  /\ CandidateElection_step = [n \in Int |-> ""]
  /\ CandidateElection_peers = [n \in Int |-> <<>>]
  /\ CandidateElection_idx = [n \in Int |-> 0]
  /\ CandidateElection_peer = [n \in Int |-> [id |-> 0, url |-> ""]]
  \* State variables for LeaderHeartbeat
  /\ LeaderHeartbeat_constructed = [n \in Int |-> FALSE]
  /\ LeaderHeartbeat_me = [n \in Int |-> [id |-> 0, url |-> ""]]
  /\ LeaderHeartbeat_step = [n \in Int |-> ""]
  /\ LeaderHeartbeat_peers = [n \in Int |-> <<>>]
  /\ LeaderHeartbeat_idx = [n \in Int |-> 0]
  /\ target = [n \in Int |-> [id |-> 0, url |-> ""]]
  /\ ae = [n \in Int |-> [term |-> 0, leaderId |-> 0, prevLogIndex |-> 0, prevLogTerm |-> 0, entryTerm |-> 0, entryValue |-> "", entriesLen |-> 0, leaderCommit |-> 0]]
  /\ session_ElectionTimeout_TimerController = [n \in Int |-> [n_TimerController \in Int |-> FALSE]]
  /\ session_TimerController_TimerHelper = [n \in Int |-> [n_TimerHelper \in Int |-> FALSE]]
  /\ sessionException = FALSE

initially(n, args) ==
  /\ ~RaftNodeMain_constructed[n]
  /\ RaftNodeMain_step' = [RaftNodeMain_step EXCEPT ![n] = "startRpcIn"]
  /\ RaftNodeMain_self' = [RaftNodeMain_self EXCEPT ![n] = TRUE.me]
  /\ theCluster' = [theCluster EXCEPT ![n] = TRUE.cluster]
  /\ RaftNodeMain_listenPort' = [RaftNodeMain_listenPort EXCEPT ![n] = TRUE]
  /\ RaftNodeMain_constructed' = [RaftNodeMain_constructed EXCEPT ![n] = TRUE]
  /\ UNCHANGED <<RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

startRpcIn(n, n_ServerInitializer, port) ==
  /\ RaftNodeMain_constructed[n]
  /\ ~ServerInitializer_constructed[n_ServerInitializer]
  /\ ((RaftNodeMain_step[n] = "startRpcIn") /\ (port = RaftNodeMain_listenPort[n]))
  /\ RaftNodeMain_step' = [RaftNodeMain_step EXCEPT ![n] = "startRpcOut"]
  /\ started' = [started EXCEPT ![n_ServerInitializer] = FALSE]
  /\ ServerInitializer_listenPort' = [ServerInitializer_listenPort EXCEPT ![n_ServerInitializer] = port]
  /\ ServerInitializer_constructed' = [ServerInitializer_constructed EXCEPT ![n_ServerInitializer] = TRUE]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

startRpcOut(n, n_RpcOutClient) ==
  /\ RaftNodeMain_constructed[n]
  /\ ~RpcOutClient_constructed[n_RpcOutClient]
  /\ (RaftNodeMain_step[n] = "startRpcOut")
  /\ RaftNodeMain_step' = [RaftNodeMain_step EXCEPT ![n] = "startCore"]
  /\ RpcOutClient_step' = [RpcOutClient_step EXCEPT ![n_RpcOutClient] = "createHttpClient"]
  /\ httpReq' = [httpReq EXCEPT ![n_RpcOutClient] = [url |-> "", method |-> "", body |-> ""]]
  /\ RpcOutClient_httpResp' = [RpcOutClient_httpResp EXCEPT ![n_RpcOutClient] = [code |-> 0, body |-> ""]]
  /\ RpcOutClient_constructed' = [RpcOutClient_constructed EXCEPT ![n_RpcOutClient] = TRUE]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

startRaftCore(n, n_RaftCore, me, cluster) ==
  /\ RaftNodeMain_constructed[n]
  /\ ~RaftCore_constructed[n_RaftCore]
  /\ (((RaftNodeMain_step[n] = "startCore") /\ (me = RaftNodeMain_self[n])) /\ (cluster = theCluster[n]))
  /\ RaftNodeMain_step' = [RaftNodeMain_step EXCEPT ![n] = "done"]
  /\ RaftCore_self' = [RaftCore_self EXCEPT ![n_RaftCore] = me]
  /\ selfId' = [selfId EXCEPT ![n_RaftCore] = me.id]
  /\ RaftCore_cluster' = [RaftCore_cluster EXCEPT ![n_RaftCore] = cluster]
  /\ clusterSize' = [clusterSize EXCEPT ![n_RaftCore] = TRUE]
  /\ currentTerm' = [currentTerm EXCEPT ![n_RaftCore] = 0]
  /\ state' = [state EXCEPT ![n_RaftCore] = "Follower"]
  /\ votedFor' = [votedFor EXCEPT ![n_RaftCore] = -1]
  /\ log' = [log EXCEPT ![n_RaftCore] = TRUE]
  /\ lastLogTermVar' = [lastLogTermVar EXCEPT ![n_RaftCore] = 0]
  /\ commitIndex' = [commitIndex EXCEPT ![n_RaftCore] = 0]
  /\ lastApplied' = [lastApplied EXCEPT ![n_RaftCore] = 0]
  /\ sm' = [sm EXCEPT ![n_RaftCore] = TRUE]
  /\ votesGranted' = [votesGranted EXCEPT ![n_RaftCore] = TRUE]
  /\ voteCount' = [voteCount EXCEPT ![n_RaftCore] = 0]
  /\ knownLeaderId' = [knownLeaderId EXCEPT ![n_RaftCore] = -1]
  /\ nextIndex' = [nextIndex EXCEPT ![n_RaftCore] = TRUE]
  /\ matchIndex' = [matchIndex EXCEPT ![n_RaftCore] = TRUE]
  /\ ackCount' = [ackCount EXCEPT ![n_RaftCore] = 0]
  /\ aePeer' = [aePeer EXCEPT ![n_RaftCore] = [id |-> 0, url |-> ""]]
  /\ aeTerm' = [aeTerm EXCEPT ![n_RaftCore] = 0]
  /\ aeLeaderId' = [aeLeaderId EXCEPT ![n_RaftCore] = 0]
  /\ aePrevIdx' = [aePrevIdx EXCEPT ![n_RaftCore] = 0]
  /\ aePrevTerm' = [aePrevTerm EXCEPT ![n_RaftCore] = 0]
  /\ aeEntryTerm' = [aeEntryTerm EXCEPT ![n_RaftCore] = 0]
  /\ aeEntryValue' = [aeEntryValue EXCEPT ![n_RaftCore] = ""]
  /\ aeEntriesLen' = [aeEntriesLen EXCEPT ![n_RaftCore] = 0]
  /\ aeLeaderCommit' = [aeLeaderCommit EXCEPT ![n_RaftCore] = 0]
  /\ aeReady' = [aeReady EXCEPT ![n_RaftCore] = FALSE]
  /\ spawnElectionTimeout' = [spawnElectionTimeout EXCEPT ![n_RaftCore] = TRUE]
  /\ spawnElection' = [spawnElection EXCEPT ![n_RaftCore] = FALSE]
  /\ spawnLeaderHeartbeat' = [spawnLeaderHeartbeat EXCEPT ![n_RaftCore] = FALSE]
  /\ resetElectionTimer' = [resetElectionTimer EXCEPT ![n_RaftCore] = FALSE]
  /\ RaftCore_constructed' = [RaftCore_constructed EXCEPT ![n_RaftCore] = TRUE]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

updateTerm(n, inTerm) ==
  /\ RaftCore_constructed[n]
  /\ TRUE
  /\ currentTerm' = [currentTerm EXCEPT ![n] = (IF (inTerm > currentTerm[n]) THEN inTerm ELSE currentTerm[n])]
  /\ state' = [state EXCEPT ![n] = (IF (inTerm > currentTerm[n]) THEN "Follower" ELSE state[n])]
  /\ votedFor' = [votedFor EXCEPT ![n] = (IF (inTerm > currentTerm[n]) THEN -1 ELSE votedFor[n])]
  /\ knownLeaderId' = [knownLeaderId EXCEPT ![n] = (IF (inTerm > currentTerm[n]) THEN -1 ELSE knownLeaderId[n])]
  /\ votesGranted' = [votesGranted EXCEPT ![n] = (IF (inTerm > currentTerm[n]) THEN (votesGranted[n] - votesGranted[n]) ELSE votesGranted[n])]
  /\ voteCount' = [voteCount EXCEPT ![n] = (IF (inTerm > currentTerm[n]) THEN 0 ELSE voteCount[n])]
  /\ resetElectionTimer' = [resetElectionTimer EXCEPT ![n] = (IF (inTerm > currentTerm[n]) THEN TRUE ELSE resetElectionTimer[n])]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, log, lastLogTermVar, commitIndex, lastApplied, sm, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

dropStaleResponse(n, respTerm, drop) ==
  /\ RaftCore_constructed[n]
  /\ ((drop => (respTerm < currentTerm[n])) /\ ((respTerm < currentTerm[n]) => drop))
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

Timeout(n, n_ElectionTimeout) ==
  /\ RaftCore_constructed[n]
  /\ ElectionTimeout_constructed[n_ElectionTimeout]
  /\ ((state[n] = "Follower") \/ (state[n] = "Candidate"))
  /\ state' = [state EXCEPT ![n] = "Candidate"]
  /\ currentTerm' = [currentTerm EXCEPT ![n] = (currentTerm[n] + 1)]
  /\ votedFor' = [votedFor EXCEPT ![n] = RaftCore_self[n].id]
  /\ votesGranted' = [votesGranted EXCEPT ![n] = TRUE]
  /\ voteCount' = [voteCount EXCEPT ![n] = 1]
  /\ knownLeaderId' = [knownLeaderId EXCEPT ![n] = -1]
  /\ spawnElection' = [spawnElection EXCEPT ![n] = TRUE]
  /\ resetElectionTimer' = [resetElectionTimer EXCEPT ![n] = FALSE]
  /\ (ElectionTimeout_step[n_ElectionTimeout] = "doTimeout")
  /\ ElectionTimeout_step' = [ElectionTimeout_step EXCEPT ![n_ElectionTimeout] = "done"]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, log, lastLogTermVar, commitIndex, lastApplied, sm, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnLeaderHeartbeat, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

startElectionTimeout(n, n_ElectionTimeout, me) ==
  /\ RaftCore_constructed[n]
  /\ ~ElectionTimeout_constructed[n_ElectionTimeout]
  /\ (spawnElectionTimeout[n] /\ (me = RaftCore_self[n]))
  /\ spawnElectionTimeout' = [spawnElectionTimeout EXCEPT ![n] = FALSE]
  /\ resetElectionTimer' = [resetElectionTimer EXCEPT ![n] = FALSE]
  /\ ElectionTimeout_selfNode' = [ElectionTimeout_selfNode EXCEPT ![n_ElectionTimeout] = me]
  /\ electTimeout' = [electTimeout EXCEPT ![n_ElectionTimeout] = (2 + me.id)]
  /\ ElectionTimeout_step' = [ElectionTimeout_step EXCEPT ![n_ElectionTimeout] = "createTimer"]
  /\ ElectionTimeout_constructed' = [ElectionTimeout_constructed EXCEPT ![n_ElectionTimeout] = TRUE]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElection, spawnLeaderHeartbeat, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

startElection(n, n_CandidateElection, me, term, candId, lastIdx, lastTerm) ==
  /\ RaftCore_constructed[n]
  /\ ~CandidateElection_constructed[n_CandidateElection]
  /\ ((((((spawnElection[n] /\ (me = RaftCore_self[n])) /\ (state[n] = "Candidate")) /\ (term = currentTerm[n])) /\ (candId = RaftCore_self[n].id)) /\ (lastIdx = TRUE)) /\ (lastTerm = lastLogTermVar[n]))
  /\ spawnElection' = [spawnElection EXCEPT ![n] = FALSE]
  /\ CandidateElection_selfNode' = [CandidateElection_selfNode EXCEPT ![n_CandidateElection] = me]
  /\ snapTerm' = [snapTerm EXCEPT ![n_CandidateElection] = term]
  /\ snapCand' = [snapCand EXCEPT ![n_CandidateElection] = candId]
  /\ snapLastIdx' = [snapLastIdx EXCEPT ![n_CandidateElection] = lastIdx]
  /\ snapLastTerm' = [snapLastTerm EXCEPT ![n_CandidateElection] = lastTerm]
  /\ CandidateElection_peers' = [CandidateElection_peers EXCEPT ![n_CandidateElection] = TRUE]
  /\ CandidateElection_idx' = [CandidateElection_idx EXCEPT ![n_CandidateElection] = 0]
  /\ CandidateElection_peer' = [CandidateElection_peer EXCEPT ![n_CandidateElection] = [id |-> 0, url |-> ""]]
  /\ CandidateElection_step' = [CandidateElection_step EXCEPT ![n_CandidateElection] = "loadCluster"]
  /\ CandidateElection_constructed' = [CandidateElection_constructed EXCEPT ![n_CandidateElection] = TRUE]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

startLeaderHeartbeat(n, n_LeaderHeartbeat, me) ==
  /\ RaftCore_constructed[n]
  /\ ~LeaderHeartbeat_constructed[n_LeaderHeartbeat]
  /\ ((spawnLeaderHeartbeat[n] /\ (me = RaftCore_self[n])) /\ (state[n] = "Leader"))
  /\ spawnLeaderHeartbeat' = [spawnLeaderHeartbeat EXCEPT ![n] = FALSE]
  /\ LeaderHeartbeat_me' = [LeaderHeartbeat_me EXCEPT ![n_LeaderHeartbeat] = me]
  /\ LeaderHeartbeat_step' = [LeaderHeartbeat_step EXCEPT ![n_LeaderHeartbeat] = "sleep"]
  /\ LeaderHeartbeat_peers' = [LeaderHeartbeat_peers EXCEPT ![n_LeaderHeartbeat] = TRUE]
  /\ LeaderHeartbeat_idx' = [LeaderHeartbeat_idx EXCEPT ![n_LeaderHeartbeat] = 0]
  /\ target' = [target EXCEPT ![n_LeaderHeartbeat] = [id |-> 0, url |-> ""]]
  /\ ae' = [ae EXCEPT ![n_LeaderHeartbeat] = [term |-> 0, leaderId |-> 0, prevLogIndex |-> 0, prevLogTerm |-> 0, entryTerm |-> 0, entryValue |-> "", entriesLen |-> 0, leaderCommit |-> 0]]
  /\ LeaderHeartbeat_constructed' = [LeaderHeartbeat_constructed EXCEPT ![n_LeaderHeartbeat] = TRUE]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

resetElectionTimer(n, n_ElectionTimeout) ==
  /\ RaftCore_constructed[n]
  /\ ElectionTimeout_constructed[n_ElectionTimeout]
  /\ resetElectionTimer[n]
  /\ resetElectionTimer' = [resetElectionTimer EXCEPT ![n] = FALSE]
  /\ (ElectionTimeout_step[n_ElectionTimeout] = "wait")
  /\ ElectionTimeout_step' = [ElectionTimeout_step EXCEPT ![n_ElectionTimeout] = "cancel"]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

handleRequestVoteRequest(n, inTerm, candidateId, lastLogIndex, lastLogTerm, outTerm, voteGranted) ==
  /\ RaftCore_constructed[n]
  /\ TRUE
  /\ votedFor' = [votedFor EXCEPT ![n] = (IF voteGranted THEN candidateId ELSE votedFor[n])]
  /\ resetElectionTimer' = [resetElectionTimer EXCEPT ![n] = (IF voteGranted THEN TRUE ELSE resetElectionTimer[n])]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

handleRequestVoteResponse(n, peerId, term, granted) ==
  /\ RaftCore_constructed[n]
  /\ ((state[n] = "Candidate") /\ (term = currentTerm[n]))
  /\ votesGranted' = [votesGranted EXCEPT ![n] = (IF granted THEN (votesGranted[n] + TRUE) ELSE votesGranted[n])]
  /\ voteCount' = [voteCount EXCEPT ![n] = (IF granted THEN (voteCount[n] + 1) ELSE voteCount[n])]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

becomeLeader(n, n_CandidateElection) ==
  /\ RaftCore_constructed[n]
  /\ CandidateElection_constructed[n_CandidateElection]
  /\ ((state[n] = "Candidate") /\ ((voteCount[n] * 2) > clusterSize[n]))
  /\ state' = [state EXCEPT ![n] = "Leader"]
  /\ knownLeaderId' = [knownLeaderId EXCEPT ![n] = RaftCore_self[n].id]
  /\ nextIndex' = [nextIndex EXCEPT ![n] = TRUE]
  /\ matchIndex' = [matchIndex EXCEPT ![n] = TRUE]
  /\ ackCount' = [ackCount EXCEPT ![n] = 0]
  /\ spawnLeaderHeartbeat' = [spawnLeaderHeartbeat EXCEPT ![n] = TRUE]
  /\ (CandidateElection_step[n_CandidateElection] = "win")
  /\ CandidateElection_step' = [CandidateElection_step EXCEPT ![n_CandidateElection] = "done"]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, spawnElectionTimeout, spawnElection, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

candidateElectionLost(n, n_CandidateElection) ==
  /\ RaftCore_constructed[n]
  /\ CandidateElection_constructed[n_CandidateElection]
  /\ ((state[n] = "Candidate") \/ (state[n] = "Follower"))
  /\ spawnElectionTimeout' = [spawnElectionTimeout EXCEPT ![n] = TRUE]
  /\ (CandidateElection_step[n_CandidateElection] = "lost")
  /\ CandidateElection_step' = [CandidateElection_step EXCEPT ![n_CandidateElection] = "done"]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

lostLeadership(n, n_LeaderHeartbeat) ==
  /\ RaftCore_constructed[n]
  /\ LeaderHeartbeat_constructed[n_LeaderHeartbeat]
  /\ (state[n] # "Leader")
  /\ spawnElectionTimeout' = [spawnElectionTimeout EXCEPT ![n] = TRUE]
  /\ (LeaderHeartbeat_step[n_LeaderHeartbeat] = "handback")
  /\ LeaderHeartbeat_step' = [LeaderHeartbeat_step EXCEPT ![n_LeaderHeartbeat] = "done"]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

clientRequest(n, value, idx) ==
  /\ RaftCore_constructed[n]
  /\ ((state[n] = "Leader") /\ (idx = (TRUE + 1)))
  /\ log' = [log EXCEPT ![n] = (log[n] + TRUE)]
  /\ lastLogTermVar' = [lastLogTermVar EXCEPT ![n] = currentTerm[n]]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

committed(n, value, idx) ==
  /\ RaftCore_constructed[n]
  /\ (((((state[n] = "Leader") /\ (idx > 0)) /\ (idx <= commitIndex[n])) /\ (idx <= TRUE)) /\ (value = TRUE))
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

leaderRedirect(n, leader) ==
  /\ RaftCore_constructed[n]
  /\ (((((state[n] # "Leader") /\ (knownLeaderId[n] >= 0)) /\ (leader.id = knownLeaderId[n])) /\ (knownLeaderId[n] < clusterSize[n])) /\ (RaftCore_cluster[n][knownLeaderId[n]] = leader))
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

noLeader(n) ==
  /\ RaftCore_constructed[n]
  /\ ((state[n] # "Leader") /\ (knownLeaderId[n] < 0))
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

getCommitted(n, stateMachine) ==
  /\ RaftCore_constructed[n]
  /\ (stateMachine = sm[n])
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

handleAppendEntriesRequest(n, inTerm, leaderId, prevLogIndex, prevLogTerm, entriesLen, entryTerm, entryValue, leaderCommit, outTerm, success, matchIdx) ==
  /\ RaftCore_constructed[n]
  /\ ((((inTerm <= currentTerm[n]) /\ (outTerm = currentTerm[n])) /\ ((entriesLen = 0) \/ (entriesLen = 1))) /\ (((~(success) /\ (matchIdx = 0)) /\ ((inTerm < currentTerm[n]) \/ ((inTerm = currentTerm[n]) /\ ((prevLogIndex > TRUE) \/ (((prevLogIndex > 0) /\ (prevLogIndex <= TRUE)) /\ (TRUE # prevLogTerm)))))) \/ (((success /\ (inTerm = currentTerm[n])) /\ ((prevLogIndex = 0) \/ (((prevLogIndex > 0) /\ (prevLogIndex <= TRUE)) /\ (TRUE = prevLogTerm)))) /\ (((entriesLen = 0) /\ (matchIdx = prevLogIndex)) \/ ((entriesLen = 1) /\ (matchIdx = (prevLogIndex + 1)))))))
  /\ knownLeaderId' = [knownLeaderId EXCEPT ![n] = (IF success THEN leaderId ELSE knownLeaderId[n])]
  /\ state' = [state EXCEPT ![n] = (IF (success /\ (inTerm = currentTerm[n])) THEN "Follower" ELSE state[n])]
  /\ resetElectionTimer' = [resetElectionTimer EXCEPT ![n] = (IF success THEN TRUE ELSE resetElectionTimer[n])]
  /\ log' = [log EXCEPT ![n] = (IF (success /\ (entriesLen = 1)) THEN (TRUE + TRUE) ELSE log[n])]
  /\ lastLogTermVar' = [lastLogTermVar EXCEPT ![n] = (IF (success /\ (entriesLen = 1)) THEN entryTerm ELSE lastLogTermVar[n])]
  /\ commitIndex' = [commitIndex EXCEPT ![n] = (IF (success /\ (leaderCommit > commitIndex[n])) THEN TRUE ELSE commitIndex[n])]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, votedFor, lastApplied, sm, votesGranted, voteCount, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

handleAppendEntriesResponse(n, peerId, inTerm, success, matchIdx) ==
  /\ RaftCore_constructed[n]
  /\ ((state[n] = "Leader") /\ (inTerm = currentTerm[n]))
  /\ matchIndex' = [matchIndex EXCEPT ![n] = [@ EXCEPT ![peerId] = (IF success THEN matchIdx ELSE (IF (peerId in matchIndex[n]) THEN matchIndex[n][peerId] ELSE 0))]]
  /\ nextIndex' = [nextIndex EXCEPT ![n] = [@ EXCEPT ![peerId] = (IF success THEN (matchIdx + 1) ELSE TRUE)]]
  /\ ackCount' = [ackCount EXCEPT ![n] = (IF (success /\ (matchIdx >= (commitIndex[n] + 1))) THEN (ackCount[n] + 1) ELSE ackCount[n])]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

advanceCommitIndex(n) ==
  /\ RaftCore_constructed[n]
  /\ ((((state[n] = "Leader") /\ (commitIndex[n] < TRUE)) /\ (TRUE = currentTerm[n])) /\ (((ackCount[n] + 1) * 2) > clusterSize[n]))
  /\ commitIndex' = [commitIndex EXCEPT ![n] = (commitIndex[n] + 1)]
  /\ ackCount' = [ackCount EXCEPT ![n] = 0]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

applyEntryToStateMachine(n) ==
  /\ RaftCore_constructed[n]
  /\ ((lastApplied[n] < commitIndex[n]) /\ (lastApplied[n] < TRUE))
  /\ sm' = [sm EXCEPT ![n] = (sm[n] + TRUE)]
  /\ lastApplied' = [lastApplied EXCEPT ![n] = (lastApplied[n] + 1)]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

getIsLeader(n, n_LeaderHeartbeat, isLeader) ==
  /\ RaftCore_constructed[n]
  /\ LeaderHeartbeat_constructed[n_LeaderHeartbeat]
  /\ ((isLeader => (state[n] = "Leader")) /\ ((state[n] = "Leader") => isLeader))
  /\ (LeaderHeartbeat_step[n_LeaderHeartbeat] = "checkLeader")
  /\ LeaderHeartbeat_step' = [LeaderHeartbeat_step EXCEPT ![n_LeaderHeartbeat] = (IF isLeader THEN "loadCluster" ELSE "handback")]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

getIsCandidate(n, n_CandidateElection, isCandidate) ==
  /\ RaftCore_constructed[n]
  /\ CandidateElection_constructed[n_CandidateElection]
  /\ ((isCandidate => (state[n] = "Candidate")) /\ ((state[n] = "Candidate") => isCandidate))
  /\ (CandidateElection_step[n_CandidateElection] = "checkQuorum")
  /\ CandidateElection_step' = [CandidateElection_step EXCEPT ![n_CandidateElection] = (IF isCandidate THEN "quorumCheck" ELSE "lost")]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

hasQuorum(n, n_CandidateElection, flag) ==
  /\ RaftCore_constructed[n]
  /\ CandidateElection_constructed[n_CandidateElection]
  /\ ((state[n] = "Candidate") /\ ((flag /\ ((voteCount[n] * 2) > clusterSize[n])) \/ (~(flag) /\ ((voteCount[n] * 2) <= clusterSize[n]))))
  /\ (CandidateElection_step[n_CandidateElection] = "quorumCheck")
  /\ CandidateElection_step' = [CandidateElection_step EXCEPT ![n_CandidateElection] = (IF flag THEN "win" ELSE "lost")]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

getCurrentTerm(n, term) ==
  /\ RaftCore_constructed[n]
  /\ (term = currentTerm[n])
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

getElectionInfo(n, term, candId, lastIdx, lastT) ==
  /\ RaftCore_constructed[n]
  /\ ((((term = currentTerm[n]) /\ (candId = RaftCore_self[n].id)) /\ (lastIdx = TRUE)) /\ (lastT = lastLogTermVar[n]))
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

\* getCluster action where RaftCore is the provider and CandidateElection is the client
getCluster_RaftCore_CandidateElection(n, n_CandidateElection, c) ==
  /\ RaftCore_constructed[n]
  /\ CandidateElection_constructed[n_CandidateElection]
  /\ (c = RaftCore_cluster[n])
  /\ (CandidateElection_step[n_CandidateElection] = "loadCluster")
  /\ CandidateElection_peers' = [CandidateElection_peers EXCEPT ![n_CandidateElection] = c]
  /\ CandidateElection_idx' = [CandidateElection_idx EXCEPT ![n_CandidateElection] = 0]
  /\ CandidateElection_step' = [CandidateElection_step EXCEPT ![n_CandidateElection] = "pickPeer"]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

\* getCluster action where RaftCore is the provider and LeaderHeartbeat is the client
getCluster_RaftCore_LeaderHeartbeat(n, n_LeaderHeartbeat, c) ==
  /\ RaftCore_constructed[n]
  /\ LeaderHeartbeat_constructed[n_LeaderHeartbeat]
  /\ (c = RaftCore_cluster[n])
  /\ (LeaderHeartbeat_step[n_LeaderHeartbeat] = "loadCluster")
  /\ LeaderHeartbeat_peers' = [LeaderHeartbeat_peers EXCEPT ![n_LeaderHeartbeat] = c]
  /\ LeaderHeartbeat_idx' = [LeaderHeartbeat_idx EXCEPT ![n_LeaderHeartbeat] = 0]
  /\ LeaderHeartbeat_step' = [LeaderHeartbeat_step EXCEPT ![n_LeaderHeartbeat] = "pickPeer"]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

getSelf(n, me) ==
  /\ RaftCore_constructed[n]
  /\ (me = RaftCore_self[n])
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

prepareAppendEntries(n, n_LeaderHeartbeat, peer) ==
  /\ RaftCore_constructed[n]
  /\ LeaderHeartbeat_constructed[n_LeaderHeartbeat]
  /\ ((((((state[n] = "Leader") /\ (peer.id # RaftCore_self[n].id)) /\ (peer.id >= 0)) /\ (peer.id < clusterSize[n])) /\ (RaftCore_cluster[n][peer.id] = peer)) /\ ~(aeReady[n]))
  /\ aePeer' = [aePeer EXCEPT ![n] = peer]
  /\ aeTerm' = [aeTerm EXCEPT ![n] = currentTerm[n]]
  /\ aeLeaderId' = [aeLeaderId EXCEPT ![n] = RaftCore_self[n].id]
  /\ aeLeaderCommit' = [aeLeaderCommit EXCEPT ![n] = commitIndex[n]]
  /\ aeReady' = [aeReady EXCEPT ![n] = TRUE]
  /\ aePrevIdx' = [aePrevIdx EXCEPT ![n] = (IF ((IF (peer.id in nextIndex[n]) THEN nextIndex[n][peer.id] ELSE (TRUE + 1)) > TRUE) THEN TRUE ELSE ((IF (peer.id in nextIndex[n]) THEN nextIndex[n][peer.id] ELSE (TRUE + 1)) - 1))]
  /\ aePrevTerm' = [aePrevTerm EXCEPT ![n] = (IF ((IF (peer.id in nextIndex[n]) THEN nextIndex[n][peer.id] ELSE (TRUE + 1)) > TRUE) THEN lastLogTermVar[n] ELSE (IF (((IF (peer.id in nextIndex[n]) THEN nextIndex[n][peer.id] ELSE (TRUE + 1)) - 1) <= 0) THEN 0 ELSE TRUE))]
  /\ aeEntriesLen' = [aeEntriesLen EXCEPT ![n] = (IF ((IF (peer.id in nextIndex[n]) THEN nextIndex[n][peer.id] ELSE (TRUE + 1)) > TRUE) THEN 0 ELSE 1)]
  /\ aeEntryTerm' = [aeEntryTerm EXCEPT ![n] = (IF ((IF (peer.id in nextIndex[n]) THEN nextIndex[n][peer.id] ELSE (TRUE + 1)) > TRUE) THEN 0 ELSE TRUE)]
  /\ aeEntryValue' = [aeEntryValue EXCEPT ![n] = (IF ((IF (peer.id in nextIndex[n]) THEN nextIndex[n][peer.id] ELSE (TRUE + 1)) > TRUE) THEN "" ELSE TRUE)]
  /\ ((LeaderHeartbeat_step[n_LeaderHeartbeat] = "prepareAE") /\ (peer = target[n_LeaderHeartbeat]))
  /\ LeaderHeartbeat_step' = [LeaderHeartbeat_step EXCEPT ![n_LeaderHeartbeat] = "needMeta"]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

getAppendEntriesForPeer(n, n_LeaderHeartbeat, peer, data) ==
  /\ RaftCore_constructed[n]
  /\ LeaderHeartbeat_constructed[n_LeaderHeartbeat]
  /\ ((aeReady[n] /\ (peer = aePeer[n])) /\ (data = [term |-> aeTerm[n], leaderId |-> aeLeaderId[n], prevLogIndex |-> aePrevIdx[n], prevLogTerm |-> aePrevTerm[n], entryTerm |-> aeEntryTerm[n], entryValue |-> aeEntryValue[n], entriesLen |-> aeEntriesLen[n], leaderCommit |-> aeLeaderCommit[n]]))
  /\ aeReady' = [aeReady EXCEPT ![n] = FALSE]
  /\ ((LeaderHeartbeat_step[n_LeaderHeartbeat] = "needMeta") /\ (peer = target[n_LeaderHeartbeat]))
  /\ ae' = [ae EXCEPT ![n_LeaderHeartbeat] = data]
  /\ LeaderHeartbeat_step' = [LeaderHeartbeat_step EXCEPT ![n_LeaderHeartbeat] = "sendAE"]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

receiveRequest(n, req) ==
  /\ ~RpcReqHandler_constructed[n]
  /\ RpcReqHandler_done' = [RpcReqHandler_done EXCEPT ![n] = FALSE]
  /\ RpcReqHandler_httpResp' = [RpcReqHandler_httpResp EXCEPT ![n] = TRUE]
  /\ RpcReqHandler_constructed' = [RpcReqHandler_constructed EXCEPT ![n] = TRUE]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

sendResponse(n, resp) ==
  /\ RpcReqHandler_constructed[n]
  /\ (~(RpcReqHandler_done[n]) /\ (resp = RpcReqHandler_httpResp[n]))
  /\ RpcReqHandler_done' = [RpcReqHandler_done EXCEPT ![n] = TRUE]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

createHttpServer(n, port) ==
  /\ ServerInitializer_constructed[n]
  /\ ((~(started[n]) /\ (ServerInitializer_listenPort[n] > 0)) /\ (port = ServerInitializer_listenPort[n]))
  /\ started' = [started EXCEPT ![n] = TRUE]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

closeHttpServer(n) ==
  /\ ServerInitializer_constructed[n]
  /\ FALSE
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

createHttpClient(n) ==
  /\ RpcOutClient_constructed[n]
  /\ (RpcOutClient_step[n] = "createHttpClient")
  /\ RpcOutClient_step' = [RpcOutClient_step EXCEPT ![n] = "idle"]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

closeHttpClient(n) ==
  /\ RpcOutClient_constructed[n]
  /\ FALSE
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

sendRpcOut(n, req) ==
  /\ RpcOutClient_constructed[n]
  /\ (RpcOutClient_step[n] = "idle")
  /\ httpReq' = [httpReq EXCEPT ![n] = req]
  /\ RpcOutClient_step' = [RpcOutClient_step EXCEPT ![n] = "send"]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

sendRequest(n, req) ==
  /\ RpcOutClient_constructed[n]
  /\ ((RpcOutClient_step[n] = "send") /\ (req = httpReq[n]))
  /\ RpcOutClient_step' = [RpcOutClient_step EXCEPT ![n] = "recv"]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

receiveResponse(n, resp) ==
  /\ RpcOutClient_constructed[n]
  /\ (RpcOutClient_step[n] = "recv")
  /\ RpcOutClient_httpResp' = [RpcOutClient_httpResp EXCEPT ![n] = resp]
  /\ RpcOutClient_step' = [RpcOutClient_step EXCEPT ![n] = "end"]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, httpReq, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

responseRpcOut(n, resp) ==
  /\ RpcOutClient_constructed[n]
  /\ ((RpcOutClient_step[n] = "end") /\ (resp = RpcOutClient_httpResp[n]))
  /\ RpcOutClient_step' = [RpcOutClient_step EXCEPT ![n] = "idle"]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

createTimer(n, n_TimerController) ==
  /\ ElectionTimeout_constructed[n]
  /\ ~TimerController_constructed[n_TimerController]
  /\ (ElectionTimeout_step[n] = "createTimer")
  /\ ElectionTimeout_step' = [ElectionTimeout_step EXCEPT ![n] = "startTimer"]
  \* Session connection semantics
  /\ IF CanStartSession_ElectionTimeout_TimerController(n, n_TimerController)
     THEN /\ session_ElectionTimeout_TimerController' = [session_ElectionTimeout_TimerController EXCEPT ![n] = [@ EXCEPT ![n_TimerController] = TRUE]]
          /\ LARGE_WRAP_AROUND' = [LARGE_WRAP_AROUND EXCEPT ![n_TimerController] = 999999]
          /\ currentId' = [currentId EXCEPT ![n_TimerController] = 0]
          /\ timing' = [timing EXCEPT ![n_TimerController] = FALSE]
          /\ start' = [start EXCEPT ![n_TimerController] = FALSE]
          /\ timeDuration' = [timeDuration EXCEPT ![n_TimerController] = 0]
          /\ ringTimeout' = [ringTimeout EXCEPT ![n_TimerController] = FALSE]
          /\ TimerController_constructed' = [TimerController_constructed EXCEPT ![n_TimerController] = TRUE]
          /\ UNCHANGED sessionException
     ELSE /\ sessionException' = TRUE
          /\ UNCHANGED <<session_ElectionTimeout_TimerController, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerController_constructed>>
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_TimerController_TimerHelper>>

startTimer(n, n_TimerController, time) ==
  /\ ElectionTimeout_constructed[n]
  /\ TimerController_constructed[n_TimerController]
  /\ (((ElectionTimeout_step[n] = "startTimer") \/ (ElectionTimeout_step[n] = "restart")) /\ (time = electTimeout[n]))
  /\ ElectionTimeout_step' = [ElectionTimeout_step EXCEPT ![n] = "wait"]
  /\ ~(timing[n_TimerController])
  /\ start' = [start EXCEPT ![n_TimerController] = TRUE]
  /\ timeDuration' = [timeDuration EXCEPT ![n_TimerController] = time]
  \* Session connection semantics
  /\ (session_ElectionTimeout_TimerController[n][n_TimerController] \/ CanStartSession_ElectionTimeout_TimerController(n, n_TimerController))
  /\ session_ElectionTimeout_TimerController' = [session_ElectionTimeout_TimerController EXCEPT ![n] = [@ EXCEPT ![n_TimerController] = TRUE]]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_TimerController_TimerHelper, sessionException>>

timeout(n, n_TimerController) ==
  /\ ElectionTimeout_constructed[n]
  /\ TimerController_constructed[n_TimerController]
  /\ (ElectionTimeout_step[n] = "wait")
  /\ ElectionTimeout_step' = [ElectionTimeout_step EXCEPT ![n] = "doTimeout"]
  /\ ringTimeout[n_TimerController]
  /\ timing' = [timing EXCEPT ![n_TimerController] = FALSE]
  /\ start' = [start EXCEPT ![n_TimerController] = FALSE]
  /\ timeDuration' = [timeDuration EXCEPT ![n_TimerController] = 0]
  /\ ringTimeout' = [ringTimeout EXCEPT ![n_TimerController] = FALSE]
  \* Session connection semantics
  /\ (session_ElectionTimeout_TimerController[n][n_TimerController] \/ CanStartSession_ElectionTimeout_TimerController(n, n_TimerController))
  /\ session_ElectionTimeout_TimerController' = [session_ElectionTimeout_TimerController EXCEPT ![n] = [@ EXCEPT ![n_TimerController] = TRUE]]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, TimerController_constructed, LARGE_WRAP_AROUND, currentId, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_TimerController_TimerHelper, sessionException>>

cancelTimer(n, n_TimerController, n_TimerHelper) ==
  /\ ElectionTimeout_constructed[n]
  /\ TimerController_constructed[n_TimerController]
  /\ (ElectionTimeout_step[n] = "cancel")
  /\ ElectionTimeout_step' = [ElectionTimeout_step EXCEPT ![n] = "restart"]
  /\ timing[n_TimerController]
  /\ currentId' = [currentId EXCEPT ![n_TimerController] = ((currentId[n_TimerController] + 1) % LARGE_WRAP_AROUND[n_TimerController])]
  /\ timing' = [timing EXCEPT ![n_TimerController] = FALSE]
  /\ start' = [start EXCEPT ![n_TimerController] = FALSE]
  /\ timeDuration' = [timeDuration EXCEPT ![n_TimerController] = 0]
  /\ ringTimeout' = [ringTimeout EXCEPT ![n_TimerController] = FALSE]
  \* Session connection semantics
  /\ (session_ElectionTimeout_TimerController[n][n_TimerController] \/ CanStartSession_ElectionTimeout_TimerController(n, n_TimerController))
  /\ session_ElectionTimeout_TimerController' = [session_ElectionTimeout_TimerController EXCEPT ![n] = [@ EXCEPT ![n_TimerController] = TRUE]]
  \* Session connection semantics
  /\ IF \E n_TimerHelper2 \in Int : session_TimerController_TimerHelper[n_TimerController][n_TimerHelper2]
     THEN /\ session_TimerController_TimerHelper[n_TimerController][n_TimerHelper]
          /\ session_TimerController_TimerHelper' = [session_TimerController_TimerHelper EXCEPT ![n_TimerController] = [@ EXCEPT ![n_TimerHelper] = FALSE]]
          /\ TimerHelper_killed' = [TimerHelper_killed EXCEPT ![n_TimerHelper] = TRUE]
     ELSE /\ UNCHANGED <<session_TimerController_TimerHelper, TimerHelper_killed>>
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, TimerController_constructed, LARGE_WRAP_AROUND, TimerHelper_constructed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, sessionException>>

timerHelperStart(n, n_TimerHelper, id, time) ==
  /\ TimerController_constructed[n]
  /\ ~TimerHelper_constructed[n_TimerHelper]
  /\ ~TimerHelper_killed[n_TimerHelper]
  /\ (((~(timing[n]) /\ start[n]) /\ (id = currentId[n])) /\ (time = timeDuration[n]))
  /\ timing' = [timing EXCEPT ![n] = TRUE]
  /\ start' = [start EXCEPT ![n] = FALSE]
  \* Session connection semantics
  /\ IF CanStartSession_TimerController_TimerHelper(n, n_TimerHelper)
     THEN /\ session_TimerController_TimerHelper' = [session_TimerController_TimerHelper EXCEPT ![n] = [@ EXCEPT ![n_TimerHelper] = TRUE]]
          /\ timerId' = [timerId EXCEPT ![n_TimerHelper] = id]
          /\ TimerHelper_done' = [TimerHelper_done EXCEPT ![n_TimerHelper] = FALSE]
          /\ TimerHelper_constructed' = [TimerHelper_constructed EXCEPT ![n_TimerHelper] = TRUE]
          /\ UNCHANGED sessionException
     ELSE /\ sessionException' = TRUE
          /\ UNCHANGED <<session_TimerController_TimerHelper, timerId, TimerHelper_done, TimerHelper_constructed>>
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timeDuration, ringTimeout, TimerHelper_killed, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController>>

timerHelperEnd(n, n_TimerHelper, id) ==
  /\ TimerController_constructed[n]
  /\ TimerHelper_constructed[n_TimerHelper]
  /\ ~TimerHelper_killed[n_TimerHelper]
  /\ timing[n]
  /\ ringTimeout' = [ringTimeout EXCEPT ![n] = (id = currentId[n])]
  /\ (~(TimerHelper_done[n_TimerHelper]) /\ (id = timerId[n_TimerHelper]))
  /\ TimerHelper_done' = [TimerHelper_done EXCEPT ![n_TimerHelper] = TRUE]
  \* Session connection semantics
  /\ (session_TimerController_TimerHelper[n][n_TimerHelper] \/ CanStartSession_TimerController_TimerHelper(n, n_TimerHelper))
  \* Session connection semantics
  /\ IF \E n_TimerHelper2 \in Int : session_TimerController_TimerHelper[n][n_TimerHelper2]
     THEN /\ session_TimerController_TimerHelper[n][n_TimerHelper]
          /\ session_TimerController_TimerHelper' = [session_TimerController_TimerHelper EXCEPT ![n] = [@ EXCEPT ![n_TimerHelper] = FALSE]]
     ELSE /\ UNCHANGED <<session_TimerController_TimerHelper>>
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, TimerHelper_constructed, TimerHelper_killed, timerId, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, sessionException>>

\* skipSelf action on CandidateElection
skipSelf_CandidateElection(n) ==
  /\ CandidateElection_constructed[n]
  /\ (((CandidateElection_step[n] = "pickPeer") /\ (CandidateElection_idx[n] < TRUE)) /\ (peers_id[CandidateElection_idx[n]] = CandidateElection_selfNode[n].id))
  /\ CandidateElection_idx' = [CandidateElection_idx EXCEPT ![n] = (CandidateElection_idx[n] + 1)]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

\* skipSelf action on LeaderHeartbeat
skipSelf_LeaderHeartbeat(n) ==
  /\ LeaderHeartbeat_constructed[n]
  /\ (((LeaderHeartbeat_step[n] = "pickPeer") /\ (LeaderHeartbeat_idx[n] < TRUE)) /\ (peers_id[LeaderHeartbeat_idx[n]] = LeaderHeartbeat_me[n].id))
  /\ LeaderHeartbeat_idx' = [LeaderHeartbeat_idx EXCEPT ![n] = (LeaderHeartbeat_idx[n] + 1)]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

allPeersDone(n) ==
  /\ CandidateElection_constructed[n]
  /\ ((CandidateElection_step[n] = "pickPeer") /\ (CandidateElection_idx[n] >= TRUE))
  /\ CandidateElection_step' = [CandidateElection_step EXCEPT ![n] = "checkQuorum"]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

\* pickPeer action on CandidateElection
pickPeer_CandidateElection(n) ==
  /\ CandidateElection_constructed[n]
  /\ (((CandidateElection_step[n] = "pickPeer") /\ (CandidateElection_idx[n] < TRUE)) /\ (peers_id[CandidateElection_idx[n]] # CandidateElection_selfNode[n].id))
  /\ CandidateElection_peer' = [CandidateElection_peer EXCEPT ![n] = CandidateElection_peers[n][CandidateElection_idx[n]]]
  /\ CandidateElection_idx' = [CandidateElection_idx EXCEPT ![n] = (CandidateElection_idx[n] + 1)]
  /\ CandidateElection_step' = [CandidateElection_step EXCEPT ![n] = "sendVote"]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_peers, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

\* pickPeer action on LeaderHeartbeat
pickPeer_LeaderHeartbeat(n) ==
  /\ LeaderHeartbeat_constructed[n]
  /\ (((LeaderHeartbeat_step[n] = "pickPeer") /\ (LeaderHeartbeat_idx[n] < TRUE)) /\ (peers_id[LeaderHeartbeat_idx[n]] # LeaderHeartbeat_me[n].id))
  /\ target' = [target EXCEPT ![n] = LeaderHeartbeat_peers[n][LeaderHeartbeat_idx[n]]]
  /\ LeaderHeartbeat_idx' = [LeaderHeartbeat_idx EXCEPT ![n] = (LeaderHeartbeat_idx[n] + 1)]
  /\ LeaderHeartbeat_step' = [LeaderHeartbeat_step EXCEPT ![n] = "prepareAE"]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_peers, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

sendVote(n) ==
  /\ CandidateElection_constructed[n]
  /\ (CandidateElection_step[n] = "sendVote")
  /\ CandidateElection_step' = [CandidateElection_step EXCEPT ![n] = "pickPeer"]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

sleep(n) ==
  /\ LeaderHeartbeat_constructed[n]
  /\ (LeaderHeartbeat_step[n] = "sleep")
  /\ LeaderHeartbeat_step' = [LeaderHeartbeat_step EXCEPT ![n] = "checkLeader"]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

roundDone(n) ==
  /\ LeaderHeartbeat_constructed[n]
  /\ ((LeaderHeartbeat_step[n] = "pickPeer") /\ (LeaderHeartbeat_idx[n] >= TRUE))
  /\ LeaderHeartbeat_step' = [LeaderHeartbeat_step EXCEPT ![n] = "sleep"]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

sendAE(n) ==
  /\ LeaderHeartbeat_constructed[n]
  /\ (LeaderHeartbeat_step[n] = "sendAE")
  /\ LeaderHeartbeat_step' = [LeaderHeartbeat_step EXCEPT ![n] = "pickPeer"]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, session_TimerController_TimerHelper, sessionException>>

EndSession_ElectionTimeout(n, n_TimerController) ==
  /\ ElectionTimeout_dead(n)
  \* Session connection semantics
  /\ session_ElectionTimeout_TimerController[n][n_TimerController]
  /\ session_ElectionTimeout_TimerController' = [session_ElectionTimeout_TimerController EXCEPT ![n] = [@ EXCEPT ![n_TimerController] = FALSE]]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_TimerController_TimerHelper, sessionException>>

EndSession_ElectionTimeout_TimerController_TimerController(n, n_TimerController) ==
  /\ TimerController_dead(n_TimerController)
  \* Session connection semantics
  /\ session_ElectionTimeout_TimerController[n][n_TimerController]
  /\ session_ElectionTimeout_TimerController' = [session_ElectionTimeout_TimerController EXCEPT ![n] = [@ EXCEPT ![n_TimerController] = FALSE]]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_TimerController_TimerHelper, sessionException>>

EndSession_TimerController_TimerHelper_TimerController(n, n_TimerHelper) ==
  /\ TimerController_dead(n)
  \* Session connection semantics
  /\ session_TimerController_TimerHelper[n][n_TimerHelper]
  /\ session_TimerController_TimerHelper' = [session_TimerController_TimerHelper EXCEPT ![n] = [@ EXCEPT ![n_TimerHelper] = FALSE]]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, sessionException>>

EndSession_TimerHelper(n, n_TimerHelper) ==
  /\ TimerHelper_dead(n_TimerHelper)
  \* Session connection semantics
  /\ session_TimerController_TimerHelper[n][n_TimerHelper]
  /\ session_TimerController_TimerHelper' = [session_TimerController_TimerHelper EXCEPT ![n] = [@ EXCEPT ![n_TimerHelper] = FALSE]]
  /\ UNCHANGED <<RaftNodeMain_constructed, RaftNodeMain_self, theCluster, RaftNodeMain_listenPort, RaftNodeMain_step, RaftCore_constructed, RaftCore_self, selfId, RaftCore_cluster, clusterSize, currentTerm, state, votedFor, log, lastLogTermVar, commitIndex, lastApplied, sm, votesGranted, voteCount, knownLeaderId, nextIndex, matchIndex, ackCount, spawnElectionTimeout, spawnElection, spawnLeaderHeartbeat, resetElectionTimer, aePeer, aeTerm, aeLeaderId, aePrevIdx, aePrevTerm, aeEntryTerm, aeEntryValue, aeEntriesLen, aeLeaderCommit, aeReady, RpcReqHandler_constructed, RpcReqHandler_httpResp, RpcReqHandler_done, ServerInitializer_constructed, started, ServerInitializer_listenPort, RpcOutClient_constructed, RpcOutClient_step, httpReq, RpcOutClient_httpResp, ElectionTimeout_constructed, ElectionTimeout_selfNode, electTimeout, ElectionTimeout_step, TimerController_constructed, LARGE_WRAP_AROUND, currentId, timing, start, timeDuration, ringTimeout, TimerHelper_constructed, TimerHelper_killed, timerId, TimerHelper_done, CandidateElection_constructed, CandidateElection_selfNode, snapTerm, snapCand, snapLastIdx, snapLastTerm, CandidateElection_step, CandidateElection_peers, CandidateElection_idx, CandidateElection_peer, LeaderHeartbeat_constructed, LeaderHeartbeat_me, LeaderHeartbeat_step, LeaderHeartbeat_peers, LeaderHeartbeat_idx, target, ae, session_ElectionTimeout_TimerController, sessionException>>

Next ==
  \/ \E n \in Int : \E args \in Seq(String) : initially(n, args)
  \/ \E n \in Int : \E n_ServerInitializer \in Int : \E port \in Int : startRpcIn(n, n_ServerInitializer, port)
  \/ \E n \in Int : \E n_RpcOutClient \in Int : startRpcOut(n, n_RpcOutClient)
  \/ \E n \in Int : \E n_RaftCore \in Int : \E me \in [id: Int, url: String] : \E cluster \in Seq([id: Int, url: String]) : startRaftCore(n, n_RaftCore, me, cluster)
  \/ \E n \in Int : \E inTerm \in Int : updateTerm(n, inTerm)
  \/ \E n \in Int : \E respTerm \in Int : \E drop \in BOOLEAN : dropStaleResponse(n, respTerm, drop)
  \/ \E n \in Int : \E n_ElectionTimeout \in Int : Timeout(n, n_ElectionTimeout)
  \/ \E n \in Int : \E n_ElectionTimeout \in Int : \E me \in [id: Int, url: String] : startElectionTimeout(n, n_ElectionTimeout, me)
  \/ \E n \in Int : \E n_CandidateElection \in Int : \E me \in [id: Int, url: String] : \E term \in Int : \E candId \in Int : \E lastIdx \in Int : \E lastTerm \in Int : startElection(n, n_CandidateElection, me, term, candId, lastIdx, lastTerm)
  \/ \E n \in Int : \E n_LeaderHeartbeat \in Int : \E me \in [id: Int, url: String] : startLeaderHeartbeat(n, n_LeaderHeartbeat, me)
  \/ \E n \in Int : \E n_ElectionTimeout \in Int : resetElectionTimer(n, n_ElectionTimeout)
  \/ \E n \in Int : \E inTerm \in Int : \E candidateId \in Int : \E lastLogIndex \in Int : \E lastLogTerm \in Int : \E outTerm \in Int : \E voteGranted \in BOOLEAN : handleRequestVoteRequest(n, inTerm, candidateId, lastLogIndex, lastLogTerm, outTerm, voteGranted)
  \/ \E n \in Int : \E peerId \in Int : \E term \in Int : \E granted \in BOOLEAN : handleRequestVoteResponse(n, peerId, term, granted)
  \/ \E n \in Int : \E n_CandidateElection \in Int : becomeLeader(n, n_CandidateElection)
  \/ \E n \in Int : \E n_CandidateElection \in Int : candidateElectionLost(n, n_CandidateElection)
  \/ \E n \in Int : \E n_LeaderHeartbeat \in Int : lostLeadership(n, n_LeaderHeartbeat)
  \/ \E n \in Int : \E value \in String : \E idx \in Int : clientRequest(n, value, idx)
  \/ \E n \in Int : \E value \in String : \E idx \in Int : committed(n, value, idx)
  \/ \E n \in Int : \E leader \in [id: Int, url: String] : leaderRedirect(n, leader)
  \/ \E n \in Int : noLeader(n)
  \/ \E n \in Int : \E stateMachine \in Seq(String) : getCommitted(n, stateMachine)
  \/ \E n \in Int : \E inTerm \in Int : \E leaderId \in Int : \E prevLogIndex \in Int : \E prevLogTerm \in Int : \E entriesLen \in Int : \E entryTerm \in Int : \E entryValue \in String : \E leaderCommit \in Int : \E outTerm \in Int : \E success \in BOOLEAN : \E matchIdx \in Int : handleAppendEntriesRequest(n, inTerm, leaderId, prevLogIndex, prevLogTerm, entriesLen, entryTerm, entryValue, leaderCommit, outTerm, success, matchIdx)
  \/ \E n \in Int : \E peerId \in Int : \E inTerm \in Int : \E success \in BOOLEAN : \E matchIdx \in Int : handleAppendEntriesResponse(n, peerId, inTerm, success, matchIdx)
  \/ \E n \in Int : advanceCommitIndex(n)
  \/ \E n \in Int : applyEntryToStateMachine(n)
  \/ \E n \in Int : \E n_LeaderHeartbeat \in Int : \E isLeader \in BOOLEAN : getIsLeader(n, n_LeaderHeartbeat, isLeader)
  \/ \E n \in Int : \E n_CandidateElection \in Int : \E isCandidate \in BOOLEAN : getIsCandidate(n, n_CandidateElection, isCandidate)
  \/ \E n \in Int : \E n_CandidateElection \in Int : \E flag \in BOOLEAN : hasQuorum(n, n_CandidateElection, flag)
  \/ \E n \in Int : \E term \in Int : getCurrentTerm(n, term)
  \/ \E n \in Int : \E term \in Int : \E candId \in Int : \E lastIdx \in Int : \E lastT \in Int : getElectionInfo(n, term, candId, lastIdx, lastT)
  \/ \E n \in Int : \E n_CandidateElection \in Int : \E c \in Seq([id: Int, url: String]) : getCluster_RaftCore_CandidateElection(n, n_CandidateElection, c)
  \/ \E n \in Int : \E n_LeaderHeartbeat \in Int : \E c \in Seq([id: Int, url: String]) : getCluster_RaftCore_LeaderHeartbeat(n, n_LeaderHeartbeat, c)
  \/ \E n \in Int : \E me \in [id: Int, url: String] : getSelf(n, me)
  \/ \E n \in Int : \E n_LeaderHeartbeat \in Int : \E peer \in [id: Int, url: String] : prepareAppendEntries(n, n_LeaderHeartbeat, peer)
  \/ \E n \in Int : \E n_LeaderHeartbeat \in Int : \E peer \in [id: Int, url: String] : \E data \in [term: Int, leaderId: Int, prevLogIndex: Int, prevLogTerm: Int, entryTerm: Int, entryValue: String, entriesLen: Int, leaderCommit: Int] : getAppendEntriesForPeer(n, n_LeaderHeartbeat, peer, data)
  \/ \E n \in Int : \E req \in [path: String, body: String] : receiveRequest(n, req)
  \/ \E n \in Int : \E resp \in [body: String, code: Int] : sendResponse(n, resp)
  \/ \E n \in Int : \E port \in Int : createHttpServer(n, port)
  \/ \E n \in Int : closeHttpServer(n)
  \/ \E n \in Int : createHttpClient(n)
  \/ \E n \in Int : closeHttpClient(n)
  \/ \E n \in Int : \E req \in [url: String, method: String, body: String] : sendRpcOut(n, req)
  \/ \E n \in Int : \E req \in [url: String, method: String, body: String] : sendRequest(n, req)
  \/ \E n \in Int : \E resp \in [body: String, code: Int] : receiveResponse(n, resp)
  \/ \E n \in Int : \E resp \in [body: String, code: Int] : responseRpcOut(n, resp)
  \/ \E n \in Int : \E n_TimerController \in Int : createTimer(n, n_TimerController)
  \/ \E n \in Int : \E n_TimerController \in Int : \E time \in Int : startTimer(n, n_TimerController, time)
  \/ \E n \in Int : \E n_TimerController \in Int : timeout(n, n_TimerController)
  \/ \E n \in Int : \E n_TimerController \in Int : \E n_TimerHelper \in Int : cancelTimer(n, n_TimerController, n_TimerHelper)
  \/ \E n \in Int : \E n_TimerHelper \in Int : \E id \in Int : \E time \in Int : timerHelperStart(n, n_TimerHelper, id, time)
  \/ \E n \in Int : \E n_TimerHelper \in Int : \E id \in Int : timerHelperEnd(n, n_TimerHelper, id)
  \/ \E n \in Int : skipSelf_CandidateElection(n)
  \/ \E n \in Int : skipSelf_LeaderHeartbeat(n)
  \/ \E n \in Int : allPeersDone(n)
  \/ \E n \in Int : pickPeer_CandidateElection(n)
  \/ \E n \in Int : pickPeer_LeaderHeartbeat(n)
  \/ \E n \in Int : sendVote(n)
  \/ \E n \in Int : sleep(n)
  \/ \E n \in Int : roundDone(n)
  \/ \E n \in Int : sendAE(n)
  \/ \E n \in Int : \E n_TimerController \in Int : EndSession_ElectionTimeout(n, n_TimerController)
  \/ \E n \in Int : \E n_TimerController \in Int : EndSession_ElectionTimeout_TimerController_TimerController(n, n_TimerController)
  \/ \E n \in Int : \E n_TimerHelper \in Int : EndSession_TimerController_TimerHelper_TimerController(n, n_TimerHelper)
  \/ \E n \in Int : \E n_TimerHelper \in Int : EndSession_TimerHelper(n, n_TimerHelper)

Spec == Init /\ [][Next]_vars

SessionIntegrity == ~sessionException

OneLeaderPerTerm == (\A n1 \in Int : (\A n2 \in Int : (((knownLeaderId[n1] = selfId[n1]) /\ (knownLeaderId[n2] = selfId[n2])) => (n1 = n2))))
====
