package com.doohh.akkaClustering.master;

import java.util.Hashtable;

import com.doohh.akkaClustering.deploy.AppConf;
import com.doohh.akkaClustering.util.Node;
import com.doohh.akkaClustering.worker.Worker;

import akka.actor.ActorRef;
import akka.actor.Address;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;
import akka.cluster.Member;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import lombok.Getter;

@Getter
public class Master extends UntypedActor {
	LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	Cluster cluster = Cluster.get(getContext().system());
	public static final String REGISTRATION_TO_MASTER = "Master registrate the worker";
	Hashtable<Address, Node> workers = new Hashtable<Address, Node>();

	private ActorRef launcher;

	// subscribe to cluster changes, MemberUp
	@Override
	public void preStart() {
		cluster.subscribe(getSelf(), MemberUp.class, UnreachableMember.class);
		this.launcher = context().actorOf(Props.create(Launcher.class), "launcher");
	}

	// re-subscribe when restart
	@Override
	public void postStop() {
		cluster.unsubscribe(getSelf());
	}

	@Override
	public void onReceive(Object message) throws Throwable {
		// clustering part
		if (message.equals(Worker.REGISTRATION_TO_WORKER)) {
			log.info("received registration msg from the worker");
			log.info("register the worker at master");
			workers.put(getSender().path().address(), new Node(getSender(), false));
			log.info("current workerTable: {}", workers);
		} else if (message instanceof MemberUp) {
			log.info("received MemberUp msg");
			MemberUp mUp = (MemberUp) message;
			log.info("send the msg to the worker for handshaking");
			register(mUp.member());
		} else if (message instanceof UnreachableMember) {
			log.info("received UnreachableMember msg");
			log.info("remove the worker from master");
			UnreachableMember mUnreachable = (UnreachableMember) message;
			workers.remove(mUnreachable.member().address());
			log.info("current workerTable: {}", workers);
		} else if (message instanceof MemberRemoved) {
			log.info("received MemberRemoved msg");
			MemberRemoved mRemoved = (MemberRemoved) message;
			log.info("Member is Removed: {}", mRemoved.member());
		} else if (message instanceof MemberEvent) {
		}

		// deploy part
		else if (message instanceof AppConf) {
			AppConf appConf = (AppConf) message;
			log.info("receive appConf msg: {}", appConf);
			launcher.tell(appConf, getSelf());
			getSender().tell("received UserAppConf instance", getSelf());
		}

		// query
		else if (message.equals("getWorkers()")) {

			getSender().tell(workers, getSelf());
		}

		else if (message instanceof String) {
			log.info("Get message = {}", (String) message);
		}

		else {
			log.info("receive unhandled msg");
			unhandled(message);
		}
	}

	void register(Member member) {
		if (member.hasRole("worker")) {
			getContext().actorSelection(member.address() + "/user/worker").tell(REGISTRATION_TO_MASTER, getSelf());
		}
	}
}
