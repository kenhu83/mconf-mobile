package org.mconf.bbb.android;


import org.mconf.bbb.BigBlueButtonClient;
import org.mconf.bbb.IBigBlueButtonClientListener;
import org.mconf.bbb.chat.ChatMessage;
import org.mconf.bbb.users.IParticipant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SlidingDrawer;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;


public class Client extends Activity implements IBigBlueButtonClientListener  {
	private static final Logger log = LoggerFactory.getLogger(Client.class);

	public static final int MENU_PUBLIC_CHAT = Menu.FIRST;
	public static final int MENU_PRIVATE_CHAT = Menu.FIRST + 1;
	public static final int MENU_QUIT = Menu.FIRST + 2;

	public static final int PUBLIC_CHAT = 1;
	public static final int PRIVATE_CHAT= 0;
	
	public static final int PUBLIC_CHAT_NOTIFICATION_ID = 77000;
	public static final int PRIVATE_CHAT_NOTIFICATION_ID = 77000;

	public static BigBlueButtonClient bbb = new BigBlueButtonClient();
	protected ContactAdapter contactAdapter;
	protected ChatAdapter chatAdapter;

	protected String myusername;
	protected SlidingDrawer slidingDrawer;
	protected Button slideHandleButton;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.contacts_list);   
		slidingDrawer = (SlidingDrawer) findViewById(R.id.slide);
		slideHandleButton = (Button) findViewById(R.id.handle);

		Bundle extras = getIntent().getExtras();
		myusername = extras.getString("username");
		Toast.makeText(getApplicationContext(),"Be welcome, " + myusername, Toast.LENGTH_SHORT).show(); 

		chatAdapter = new ChatAdapter(this);
		final ListView chatListView = (ListView)findViewById(R.id.messages);
		chatListView.setAdapter(chatAdapter);

		final ListView contactListView = (ListView)findViewById(R.id.list);
		contactAdapter = new ContactAdapter(this);
		contactListView.setAdapter(contactAdapter);

		bbb.addListener(this);

		Button send = (Button)findViewById(R.id.sendMessage);
		send.setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View viewParam) {
				EditText chatMessageEdit = (EditText) findViewById(R.id.chatMessage);
				String chatMessage = chatMessageEdit.getText().toString();
				bbb.sendPublicChatMessage(chatMessage);
				chatMessageEdit.setText("");
			}
		});

		contactListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {

				final Contact contact = (Contact) contactAdapter.getItem(position); 

				//se o ID da pessoa clicada for diferente do meu ID
				if (contact.getUserId() != bbb.getHandler().getMyUserId())
					startPrivateChat(contact);
			}

		});
	}
	
	private void startPrivateChat(Contact contact) {
		Intent intent = contact.getIntent();		
		if (intent == null) {
			intent = new Intent(getApplicationContext(), PrivateChat.class);
			intent.putExtra("username", contact.getName());
			intent.putExtra("userId", contact.getUserId());
			contact.setIntent(intent);
		}
//		startActivityForResult(intent, 0);
		startActivity(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_QUIT, 0, "Quit");
		return result;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case MENU_QUIT:
				bbb.removeListener(this);
				bbb.disconnect();
				finish();
				return true;
			}

		return super.onOptionsItemSelected(item);
	}
	
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if (keyCode == KeyEvent.KEYCODE_BACK) {
    		log.debug("KEYCODE_BACK");
    		moveTaskToBack(true);
    		return true;
    	}    		
    	return super.onKeyDown(keyCode, event);
    }

	@Override
	public void onConnected() {
		// TODO Auto-generated method stub

	}
	@Override
	public void onDisconnected() {

	}
	@Override
	public void onKickUserCallback() {
		// TODO Auto-generated method stub

	}
	@Override
	public void onParticipantJoined(final IParticipant p) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				contactAdapter.addSection(p);
				contactAdapter.notifyDataSetChanged();
			}
		});		
	}
	@Override
	public void onParticipantLeft(final IParticipant p) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				contactAdapter.removeSection(p);
				contactAdapter.notifyDataSetChanged();		
			}
		});
	}

	@Override
	public void onPrivateChatMessage(ChatMessage message, IParticipant source) {
		if (message.getUserId() == bbb.getHandler().getMyUserId())
			return;
		
		Contact contact = contactAdapter.getUserById(source.getUserId());
		
		String title = "New message arrived";

		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		Notification notification = new Notification(R.drawable.icon_bbb, title, System.currentTimeMillis());
		
		
		Intent notificationIntent = contact.getIntent();
		PendingIntent contentIntent = null;
		if (notificationIntent == null) {
			log.debug("creating a new intent");
			notificationIntent = new Intent(getApplicationContext(), PrivateChat.class);
			notificationIntent.putExtra("username", contact.getName());
			notificationIntent.putExtra("userId", contact.getUserId());
			contact.setIntent(notificationIntent);

			contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, Intent.FLAG_ACTIVITY_NEW_TASK);		
		} else {
			log.debug("reusing intent");
			contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);		
		}

		
		notification.setLatestEventInfo(getApplicationContext(), title, "from " + source.getName(), contentIntent);
		notificationManager.notify(PRIVATE_CHAT_NOTIFICATION_ID + message.getUserId(), notification);
		
//		showNotification(message, source, true);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		log.debug("onActivityResult");
	}
	
	public void showNotification(ChatMessage message, IParticipant source, boolean privateChat) {
		String title = "New message arrived";

		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		Notification notification = new Notification(R.drawable.icon_bbb, title, System.currentTimeMillis());
		
		Intent notificationIntent = null;
		if (privateChat) {
			notificationIntent = new Intent(this, PrivateChat.class);
			notificationIntent.putExtra("chatMessage", message.getMessage());
			notificationIntent.putExtra("contactName", source.getName());
			notificationIntent.putExtra("myName", myusername);
			notificationIntent.putExtra("userID", source.getUserId());

			PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
			notification.setLatestEventInfo(getApplicationContext(), title, "from " + source.getName(), contentIntent);
			notificationManager.notify(PRIVATE_CHAT_NOTIFICATION_ID, notification);
		} else {
//			notificationIntent = new Intent(this, OnPublicChatMessage.class);
//
//			PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
//			notification.setLatestEventInfo(this, title, "from " + source.getName(), contentIntent);
//			notificationManager.notify(PUBLIC_CHAT_NOTIFICATION_ID, notification);
		}
	}
	
	@Override
	public void onPublicChatMessage(final ChatMessage message, final IParticipant source) {
		// \TODO implement notification on public chat messages
//		showNotification(message, source, false);
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				chatAdapter.add(message);
				chatAdapter.notifyDataSetChanged();
			}
		});
	}

	@Override
	public void onParticipantStatusChangePresenter(final IParticipant p) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				contactAdapter.setPresenterStatus(new Contact(p));
				contactAdapter.notifyDataSetChanged();
			}
		});
	}

	@Override
	public void onParticipantStatusChangeHasStream(final IParticipant p) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				contactAdapter.setStreamStatus(new Contact(p));
				contactAdapter.notifyDataSetChanged();
			}
		});
	}

	@Override
	public void onParticipantStatusChangeRaiseHand(final IParticipant p) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				contactAdapter.setRaiseHandStatus(new Contact(p));			
				contactAdapter.notifyDataSetChanged();
			}
		});
	}

}