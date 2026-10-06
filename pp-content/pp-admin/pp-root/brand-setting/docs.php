<?php
global $site_url, $path_admin;
?>
<div class="page-header d-print-none mb-4">
    <div class="container-xl">
        <div class="row g-2 align-items-center">
            <div class="col">
                <div class="mb-1">
                    <ol class="breadcrumb" aria-label="breadcrumbs">
                        <li class="breadcrumb-item"><a href="javascript:void(0)" onclick="load_content('Dashboard','<?php echo $site_url.$path_admin ?>/dashboard','nav-item-dashboard')">Dashboard</a></li>
                        <li class="breadcrumb-item"><a href="javascript:void(0)" onclick="load_content('API Settings','<?php echo $site_url.$path_admin ?>/brand-setting/api-setting','nav-item-brand-setting')">API Settings</a></li>
                        <li class="breadcrumb-item active"><a href="javascript:void(0)">Documentation</a></li>
                    </ol>
                </div>
                <h2 class="page-title fw-bold" style="color: #0f172a;">API Documentation & Developer Guide</h2>
                <p class="text-muted mt-1">Complete API reference for integrating checkout, payment verification, and webhook notifications.</p>
            </div>
            <div class="col-auto ms-auto d-print-none">
                <button class="btn btn-primary" onclick="load_content('API Settings','<?php echo $site_url.$path_admin ?>/brand-setting/api-setting','nav-item-brand-setting')">
                    <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="icon me-1"><path stroke="none" d="M0 0h24v24H0z" fill="none"/><path d="M12 5l0 14" /><path d="M5 12l14 0" /></svg>
                    Manage API Keys
                </button>
            </div>
        </div>
    </div>
</div>

<div class="page-body">
    <div class="container-xl">
        <div class="row g-4">
            <!-- Navigation Sidebar -->
            <div class="col-lg-3">
                <div class="card shadow-sm sticky-top" style="top: 80px; z-index: 10;">
                    <div class="card-header bg-white py-3">
                        <h4 class="card-title m-0 fw-bold">Table of Contents</h4>
                    </div>
                    <div class="list-group list-group-flush" id="docs-nav">
                        <a href="#section-overview" class="list-group-item list-group-item-action fw-medium">1. Overview & Base URL</a>
                        <a href="#section-auth" class="list-group-item list-group-item-action fw-medium">2. Authentication</a>
                        <a href="#section-checkout" class="list-group-item list-group-item-action fw-medium">3. Create Payment (Checkout)</a>
                        <a href="#section-verify" class="list-group-item list-group-item-action fw-medium">4. Verify Payment</a>
                        <a href="#section-refund" class="list-group-item list-group-item-action fw-medium">5. Refund Payment</a>
                        <a href="#section-webhooks" class="list-group-item list-group-item-action fw-medium">6. Webhook Notifications</a>
                        <a href="#section-errors" class="list-group-item list-group-item-action fw-medium">7. Response & Error Codes</a>
                    </div>
                </div>
            </div>

            <!-- Main Documentation Content -->
            <div class="col-lg-9">
                <!-- Section 1: Overview -->
                <div class="card shadow-sm mb-4" id="section-overview">
                    <div class="card-header bg-white py-3">
                        <h3 class="card-title fw-bold text-primary mb-0">1. Overview & Base URL</h3>
                    </div>
                    <div class="card-body">
                        <p class="text-secondary">
                            The Payment Gateway API allows developers to seamlessly process payments via Mobile Banking (bKash, Nagad, Rocket) and Global Payment gateways. All API endpoints communicate via standard HTTP requests and return JSON responses.
                        </p>
                        
                        <div class="mb-3">
                            <label class="form-label fw-bold">Base API Endpoint:</label>
                            <div class="input-group">
                                <span class="input-group-text bg-light fw-bold text-uppercase">POST / GET</span>
                                <input type="text" class="form-control font-monospace bg-light fw-semibold" value="<?php echo $site_url; ?>api" readonly>
                                <button class="btn btn-outline-secondary" onclick="copyContent('<?php echo $site_url; ?>api', 'Copied!', 'Base URL copied')">
                                    Copy URL
                                </button>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Section 2: Authentication -->
                <div class="card shadow-sm mb-4" id="section-auth">
                    <div class="card-header bg-white py-3">
                        <h3 class="card-title fw-bold text-primary mb-0">2. Authentication</h3>
                    </div>
                    <div class="card-body">
                        <p class="text-secondary">
                            Every request to the payment API must include your unique <strong>API Secret Key</strong>. API Keys can be generated and managed under <a href="javascript:void(0)" onclick="load_content('API Settings','<?php echo $site_url.$path_admin ?>/brand-setting/api-setting','nav-item-brand-setting')">API Settings</a>.
                        </p>

                        <div class="alert alert-info border-0 shadow-xs mb-3">
                            <strong>Security Note:</strong> Always keep your Secret Key safe and never share it publicly or expose it in client-side front-end scripts.
                        </div>

                        <h5 class="fw-bold mt-4">Header Authentication Format:</h5>
                        <pre class="bg-dark text-light p-3 rounded font-monospace"><code>Authorization: Bearer YOUR_API_SECRET_KEY
Content-Type: application/json</code></pre>
                    </div>
                </div>

                <!-- Section 3: Create Checkout -->
                <div class="card shadow-sm mb-4" id="section-checkout">
                    <div class="card-header bg-white py-3">
                        <h3 class="card-title fw-bold text-primary mb-0">3. Create Payment (Redirect Checkout)</h3>
                    </div>
                    <div class="card-body">
                        <p class="text-secondary">Use this endpoint to generate a hosted checkout payment session URL.</p>
                        
                        <div class="mb-3">
                            <span class="badge bg-success-lt fs-6 mb-2">POST</span>
                            <div class="font-monospace p-2 bg-light rounded border fw-bold"><?php echo $site_url; ?>api/checkout/redirect</div>
                        </div>

                        <h5 class="fw-bold mt-4">Request Parameters (JSON Payload):</h5>
                        <div class="table-responsive">
                            <table class="table table-bordered align-middle">
                                <thead class="table-light">
                                    <tr>
                                        <th>Field</th>
                                        <th>Type</th>
                                        <th>Required</th>
                                        <th>Description</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr>
                                        <td class="font-monospace text-primary">amount</td>
                                        <td>Float / Decimal</td>
                                        <td><span class="badge bg-danger">Required</span></td>
                                        <td>The payment amount (e.g. <code>100.00</code>).</td>
                                    </tr>
                                    <tr>
                                        <td class="font-monospace text-primary">currency</td>
                                        <td>String</td>
                                        <td><span class="badge bg-danger">Required</span></td>
                                        <td>ISO Currency code (e.g. <code>BDT</code>, <code>USD</code>).</td>
                                    </tr>
                                    <tr>
                                        <td class="font-monospace text-primary">customer_name</td>
                                        <td>String</td>
                                        <td><span class="badge bg-secondary">Optional</span></td>
                                        <td>Full name of the paying customer.</td>
                                    </tr>
                                    <tr>
                                        <td class="font-monospace text-primary">customer_email</td>
                                        <td>String</td>
                                        <td><span class="badge bg-secondary">Optional</span></td>
                                        <td>Email address for sending payment receipts.</td>
                                    </tr>
                                    <tr>
                                        <td class="font-monospace text-primary">redirect_url</td>
                                        <td>String (URL)</td>
                                        <td><span class="badge bg-danger">Required</span></td>
                                        <td>URL where the customer is redirected after payment completion.</td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>

                        <h5 class="fw-bold mt-4">Sample JSON Response:</h5>
                        <pre class="bg-dark text-light p-3 rounded font-monospace"><code>{
  "status": "success",
  "message": "Checkout URL generated successfully",
  "data": {
    "trx_id": "TRX_982317491823",
    "payment_url": "<?php echo $site_url; ?>checkout/TRX_982317491823",
    "amount": 100.00,
    "currency": "BDT"
  }
}</code></pre>
                    </div>
                </div>

                <!-- Section 4: Verify Payment -->
                <div class="card shadow-sm mb-4" id="section-verify">
                    <div class="card-header bg-white py-3">
                        <h3 class="card-title fw-bold text-primary mb-0">4. Verify Payment Status</h3>
                    </div>
                    <div class="card-body">
                        <p class="text-secondary">Verify the exact status of any payment using its Transaction ID (<code>trx_id</code>).</p>
                        
                        <div class="mb-3">
                            <span class="badge bg-primary-lt fs-6 mb-2">POST</span>
                            <div class="font-monospace p-2 bg-light rounded border fw-bold"><?php echo $site_url; ?>api/verify-payment</div>
                        </div>

                        <h5 class="fw-bold mt-4">Request Body:</h5>
                        <pre class="bg-dark text-light p-3 rounded font-monospace"><code>{
  "trx_id": "TRX_982317491823"
}</code></pre>

                        <h5 class="fw-bold mt-4">Sample Success Response:</h5>
                        <pre class="bg-dark text-light p-3 rounded font-monospace"><code>{
  "status": "success",
  "payment_status": "COMPLETED",
  "trx_id": "TRX_982317491823",
  "amount": "100.00",
  "currency": "BDT",
  "sender_number": "01700000000",
  "method": "bKash Personal",
  "date": "2026-10-06 23:45:00"
}</code></pre>
                    </div>
                </div>

                <!-- Section 5: Refund Payment -->
                <div class="card shadow-sm mb-4" id="section-refund">
                    <div class="card-header bg-white py-3">
                        <h3 class="card-title fw-bold text-primary mb-0">5. Refund Payment</h3>
                    </div>
                    <div class="card-body">
                        <p class="text-secondary">Initiate a refund for a previously completed transaction.</p>
                        
                        <div class="mb-3">
                            <span class="badge bg-warning-lt fs-6 mb-2">POST</span>
                            <div class="font-monospace p-2 bg-light rounded border fw-bold"><?php echo $site_url; ?>api/refund-payment</div>
                        </div>

                        <h5 class="fw-bold mt-4">Request Body:</h5>
                        <pre class="bg-dark text-light p-3 rounded font-monospace"><code>{
  "trx_id": "TRX_982317491823",
  "reason": "Customer cancellation request"
}</code></pre>
                    </div>
                </div>

                <!-- Section 6: Webhooks -->
                <div class="card shadow-sm mb-4" id="section-webhooks">
                    <div class="card-header bg-white py-3">
                        <h3 class="card-title fw-bold text-primary mb-0">6. Webhook Notifications</h3>
                    </div>
                    <div class="card-body">
                        <p class="text-secondary">
                            Configure webhooks to receive real-time HTTP POST notifications on your server when a payment status updates (e.g. <code>COMPLETED</code> or <code>FAILED</code>).
                        </p>
                        <pre class="bg-dark text-light p-3 rounded font-monospace"><code>{
  "event": "payment.completed",
  "trx_id": "TRX_982317491823",
  "status": "COMPLETED",
  "amount": 100.00,
  "currency": "BDT",
  "timestamp": 1791308700
}</code></pre>
                    </div>
                </div>

                <!-- Section 7: Error Codes -->
                <div class="card shadow-sm mb-4" id="section-errors">
                    <div class="card-header bg-white py-3">
                        <h3 class="card-title fw-bold text-primary mb-0">7. Response & Error Codes</h3>
                    </div>
                    <div class="card-body">
                        <div class="table-responsive">
                            <table class="table table-striped align-middle">
                                <thead>
                                    <tr>
                                        <th>Code</th>
                                        <th>Status</th>
                                        <th>Description</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr>
                                        <td><code>200 OK</code></td>
                                        <td><span class="badge bg-success">Success</span></td>
                                        <td>Request processed successfully.</td>
                                    </tr>
                                    <tr>
                                        <td><code>401 Unauthorized</code></td>
                                        <td><span class="badge bg-danger">Auth Error</span></td>
                                        <td>Invalid or missing API key.</td>
                                    </tr>
                                    <tr>
                                        <td><code>400 Bad Request</code></td>
                                        <td><span class="badge bg-warning">Invalid Data</span></td>
                                        <td>Missing required fields (e.g. amount or redirect_url).</td>
                                    </tr>
                                    <tr>
                                        <td><code>404 Not Found</code></td>
                                        <td><span class="badge bg-secondary">Not Found</span></td>
                                        <td>Transaction ID does not exist.</td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>

            </div>
        </div>
    </div>
</div>
