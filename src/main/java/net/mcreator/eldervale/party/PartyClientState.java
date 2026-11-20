package net.mcreator.eldervale.party;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * What the client knows about its party: the last snapshot from the server and the toast queue.
 * Plain data (no Minecraft client classes), so it is safe to reference from the payload handlers.
 * Only touched from the client thread.
 */
public final class PartyClientState {
	public static boolean inParty = false;
	public static String partyName = "";
	public static UUID leader = new UUID(0L, 0L);
	public static int maxMembers = 5;
	public static List<PartyStatePayload.MemberInfo> members = List.of();

	public static String inviteParty = "";
	public static String inviteFrom = "";
	public static long inviteDeadlineMs = 0L;

	/** Called by the Party screen to refresh itself when a new snapshot arrives. */
	public static Runnable listener;

	private static final long TOAST_LIFE_MS = 4700L;
	private static final int TOAST_MAX = 4;

	public static final class Toast {
		public final int kind;
		public final String title;
		public final String text;
		public final long created = System.currentTimeMillis();
		public boolean soundPlayed = false;

		Toast(int kind, String title, String text) {
			this.kind = kind;
			this.title = title;
			this.text = text;
		}
	}

	private static final List<Toast> TOASTS = new ArrayList<>();

	private PartyClientState() {
	}

	public static void update(PartyStatePayload p) {
		inParty = p.inParty();
		partyName = p.partyName();
		leader = p.leader();
		maxMembers = p.maxMembers();
		members = p.members();
		inviteParty = p.inviteParty();
		inviteFrom = p.inviteFrom();
		inviteDeadlineMs = p.inviteSeconds() > 0 ? System.currentTimeMillis() + p.inviteSeconds() * 1000L : 0L;
		if (listener != null)
			listener.run();
	}

	public static boolean hasInvite() {
		return !inviteFrom.isEmpty() && System.currentTimeMillis() < inviteDeadlineMs;
	}

	public static boolean isLeader(UUID me) {
		return inParty && leader.equals(me);
	}

	public static void addToast(int kind, String title, String text) {
		TOASTS.add(new Toast(kind, title, text));
		while (TOASTS.size() > TOAST_MAX)
			TOASTS.remove(0);
	}

	/** Toasts that are still alive (expired ones are removed). */
	public static List<Toast> activeToasts() {
		long now = System.currentTimeMillis();
		TOASTS.removeIf(t -> now - t.created > TOAST_LIFE_MS);
		return new ArrayList<>(TOASTS);
	}

	public static long toastLifeMs() {
		return TOAST_LIFE_MS;
	}

	public static void reset() {
		inParty = false;
		partyName = "";
		leader = new UUID(0L, 0L);
		members = List.of();
		inviteParty = "";
		inviteFrom = "";
		inviteDeadlineMs = 0L;
		TOASTS.clear();
	}
}
