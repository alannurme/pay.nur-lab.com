<?php
if (!defined('PipraPay_INIT')) {
    http_response_code(403);
    exit('Direct access not allowed');
}

if (!canAccessPage(json_decode($global_response_permission['response'][0]['permission'], true), 'system_settings', $global_user_response['response'][0]['role'])) {
    http_response_code(403);
    exit('Access denied. You need permission to perform this action. Please contact the admin.');
}

if (!hasPermission(json_decode($global_response_permission['response'][0]['permission'], true), 'system_settings', 'manage_general', $global_user_response['response'][0]['role'])) {
    http_response_code(403);
    exit('Access denied. You need permission to perform this action. Please contact the admin.');
}

$providers = senderWhitelist();
?>

<div class="page-header d-print-none" aria-label="Page header">
    <div class="container-xl">
        <div class="row g-2 align-items-center">
            <div class="col">
                <div class="page-pretitle">
                    <ol class="breadcrumb breadcrumb-arrow mb-0">
                        <li class="breadcrumb-item"><a href="javascript:void(0)" onclick="load_content('System Settings','<?php echo $site_url.$path_admin ?>/system-settings','nav-item-system-settings')">System Settings</a></li>
                        <li class="breadcrumb-item active"><a href="javascript:void(0)">SMS Senders & Whitelist</a></li>
                    </ol>
                </div>
                <h2 class="page-title d-flex align-items-center gap-2">
                    <svg xmlns="http://www.w3.org/2000/svg" class="icon text-primary" width="24" height="24" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round"><path stroke="none" d="M0 0h24v24H0z" fill="none"/><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2h-16c-1.1 0-2-.9-2-2v-12c0-1.1.9-2 2-2z"/><path d="M4 8l8 5l8 -5"/></svg>
                    SMS Senders & Whitelist
                </h2>
            </div>
        </div>
    </div>
</div>

<div class="page-body">
    <div class="container-xl">
        <div class="row g-gs">
            <div class="col-12 col-xxl-4">
                <h2 class="card-title m-0 mb-1">SMS Sender Configuration</h2>
                <p class="text-muted">Configure the allowed SMS sender IDs or phone numbers for each MFS and Bank gateway. Separate multiple senders with a comma (e.g. <code>bkash, 16247, BKASH</code>).</p>
                <div class="alert alert-info">
                    <div class="d-flex">
                        <div>
                            <svg xmlns="http://www.w3.org/2000/svg" class="icon alert-icon" width="24" height="24" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round"><path stroke="none" d="M0 0h24v24H0z" fill="none"/><path d="M12 9v4"/><path d="M12 16v.01"/><path d="M12 3c-4.97 0-9 4.03-9 9s4.03 9 9 9 9-4.03 9-9-4.03-9-9-9z"/></svg>
                        </div>
                        <div>
                            <strong>How it works:</strong> The companion app forwards SMS from these sender IDs. If an SMS originates from any of the listed sender IDs for a provider, the system will verify and process the transaction automatically.
                        </div>
                    </div>
                </div>
            </div>
            <div class="col-12 col-xxl-8">
                <div class="card p-2">
                    <div class="card-body">
                        <form id="form-sms-senders">
                            <div class="row g-3">
                                <?php foreach ($providers as $key => $prov): ?>
                                    <?php 
                                        $currentSenders = implode(', ', $prov['senders'] ?? []);
                                    ?>
                                    <div class="col-lg-6">
                                        <div class="form-group">
                                            <label for="sender_<?= $key ?>" class="form-label d-flex align-items-center justify-content-between">
                                                <span><strong><?= htmlspecialchars($prov['name']) ?></strong> Sender ID(s)</span>
                                                <span class="badge bg-blue-lt"><?= strtoupper($key) ?></span>
                                            </label>
                                            <input type="text" class="form-control" id="sender_<?= $key ?>" name="senders[<?= $key ?>]" value="<?= htmlspecialchars($currentSenders) ?>" placeholder="e.g. <?= $key ?>">
                                            <small class="form-hint">Comma separated sender names or numbers</small>
                                        </div>
                                    </div>
                                <?php endforeach; ?>

                                <div class="col-lg-12 mt-4 text-end">
                                    <button type="button" class="btn btn-primary btn-save-sms-senders">
                                        <svg xmlns="http://www.w3.org/2000/svg" class="icon" width="24" height="24" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round"><path stroke="none" d="M0 0h24v24H0z" fill="none"/><path d="M6 4h10l4 4v10a2 2 0 0 1 -2 2h-12a2 2 0 0 1 -2 -2v-12a2 2 0 0 1 2 -2"/><path d="M12 14m-2 0a2 2 0 1 0 4 0a2 2 0 1 0 -4 0"/><path d="M14 4l0 4l-6 0l0 -4"/></svg>
                                        Save SMS Senders
                                    </button>
                                </div>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<script data-cfasync="false">
    $('.btn-save-sms-senders').click(function () {
        var csrf_token_default = $('input[name="csrf_token_default"]').val();
        var $btn = $(this);
        var originalHtml = $btn.html();

        var formData = $('#form-sms-senders').serializeArray();
        formData.push({ name: 'action', value: 'system-settings-sms-senders-save' });
        formData.push({ name: 'csrf_token', value: csrf_token_default });

        $btn.prop('disabled', true).html('<div class="spinner-border spinner-border-sm me-1" role="status"></div> Saving...');

        $.ajax({
            type: 'POST',
            url: '<?php echo $site_url.$path_admin ?>/dashboard',
            data: formData,
            dataType: 'json',
            success: function (response) {
                $btn.prop('disabled', false).html(originalHtml);

                if (response.csrf_token) {
                    $('input[name="csrf_token"], input[name="csrf_token_default"]').val(response.csrf_token);
                }

                if (response.status === 'true') {
                    createToast({
                        title: response.title || 'Saved',
                        description: response.message || 'SMS senders saved successfully.',
                        svg: `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#5f38f9" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="icon icon-tabler icons-tabler-outline icon-tabler-circle-check"><path stroke="none" d="M0 0h24v24H0z" fill="none"/><path d="M12 12m-9 0a9 9 0 1 0 18 0a9 9 0 1 0 -18 0" /><path d="M9 12l2 2l4 -4" /></svg>`,
                        timeout: 5000,
                        top: 70
                    });
                } else {
                    createToast({
                        title: response.title || 'Save Failed',
                        description: response.message || 'Failed to save SMS senders.',
                        svg: `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#d63939" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="icon icon-tabler icons-tabler-outline icon-tabler-exclamation-circle"><path stroke="none" d="M0 0h24v24H0z" fill="none"/><path d="M12 12m-9 0a9 9 0 1 0 18 0a9 9 0 1 0 -18 0" /><path d="M12 9v4" /><path d="M12 16v.01" /></svg>`,
                        timeout: 6000,
                        top: 70
                    });
                }
            },
            error: function () {
                $btn.prop('disabled', false).html(originalHtml);
                createToast({
                    title: 'Connection Error',
                    description: 'Could not connect to server.',
                    svg: `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#d63939" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="icon icon-tabler icons-tabler-outline icon-tabler-exclamation-circle"><path stroke="none" d="M0 0h24v24H0z" fill="none"/><path d="M12 12m-9 0a9 9 0 1 0 18 0a9 9 0 1 0 -18 0" /><path d="M12 9v4" /><path d="M12 16v.01" /></svg>`,
                    timeout: 6000,
                    top: 70
                });
            }
        });
    });
</script>
