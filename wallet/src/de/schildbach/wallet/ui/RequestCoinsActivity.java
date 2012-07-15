/*
 * Copyright 2011-2012 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.schildbach.wallet.ui;

import java.io.IOException;
import java.util.UUID;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.webkit.WebView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.google.bitcoin.core.ProtocolException;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Wallet;

import de.schildbach.wallet.Constants;
import de.schildbach.wallet_test.R;

/**
 * @author Andreas Schildbach
 */
public final class RequestCoinsActivity extends AbstractWalletActivity {
	private static final int DIALOG_HELP = 0;
	private BluetoothAdapter bluetoothAdapter;
	private BluetoothAcceptThread acceptThread;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.request_coins_content);

		final ActionBar actionBar = getSupportActionBar();
		actionBar.setTitle(R.string.request_coins_activity_title);
		actionBar.setDisplayHomeAsUpEnabled(true);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
			handleBluetooth();
		}
	}

	@Override
	protected void onDestroy() {

		if (acceptThread != null) {
			acceptThread.stopAccepting();
		}
		super.onDestroy();
	}

	// @TargetApi(10)
	private void handleBluetooth() {

		// For receiving a payment via Bluetooth
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (bluetoothAdapter == null) {
			Toast.makeText(this, "No Bluetooth is available", Toast.LENGTH_LONG)
					.show();
			System.out.println("no bluetooth is available");
		} else {
			System.out.println("bluetooth is available");
			if (bluetoothAdapter.isEnabled()) {
				System.out.println("bluetooth is enabled");
				RequestCoinsFragment fragment = (RequestCoinsFragment) getSupportFragmentManager()
						.findFragmentById(R.id.request_coins_fragment);
				fragment.update(bluetoothAdapter.getAddress());
				
				BluetoothServerSocket listeningSocket = null;
				try {
					listeningSocket = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(
									"Bitcoin Transaction Submission", Constants.BLUETOOTH_UUID);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				acceptThread = new BluetoothAcceptThread(
						listeningSocket) {
					@Override
					public void handleTx(final byte[] msg) {
						runOnUiThread(new Runnable() {

							public void run() {
								// FIXME: implement
								System.out.println("BTTX bluetooth message arrived");
								try {
									Transaction tx = new Transaction(Constants.NETWORK_PARAMETERS, msg);
									System.out.println("BTTX "+tx);
									getWalletApplication().getWallet().receivePending(tx);
								} catch (Exception e){
									e.printStackTrace();	
								}
							}
						});
					}

				};
				acceptThread.start();
			} else {
				Toast.makeText(this, "Please enable Bluetooth",
						Toast.LENGTH_LONG).show();
			}
		}
	}
	

	
	@Override
	public boolean onCreateOptionsMenu(final Menu menu)
	{
		getSupportMenuInflater().inflate(R.menu.request_coins_activity_options, menu);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item)
	{
		switch (item.getItemId())
		{
			case android.R.id.home:
				finish();
				return true;

			case R.id.request_coins_options_help:
				showDialog(DIALOG_HELP);
				return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected Dialog onCreateDialog(final int id)
	{
		final WebView webView = new WebView(this);
		webView.loadUrl("file:///android_asset/help_request_coins" + languagePrefix() + ".html");

		final Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(webView);
		dialog.setCanceledOnTouchOutside(true);

		return dialog;
	}
}
