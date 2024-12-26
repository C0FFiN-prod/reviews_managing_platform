const AdminModule = (() => {
  let deleteUserId;
  let modal;
  let filler;
  let userManagementTable;
  let checkUsersTable;
  document.addEventListener('DOMContentLoaded', () => {
    modal = new bootstrap.Modal(document.getElementById('confirmModal'));
    userManagementTable = document.getElementById("userManagementTable");
    filler = document.createElement("tr");
    filler.innerHTML = `<td colspan="5" class="fw-semibold text-center">No more users left (&gt;_&lt;)</td>`;
    checkUsersTable = () => {
      if (!userManagementTable.innerHTML.trim()) {
        userManagementTable.appendChild(filler)
      } else filler = userManagementTable.removeChild(filler);
    };
    // checkUsersTable();

    document.getElementById('confirmModal-body').innerHTML =
      `<p>Are you sure you want to delete this user? This action cannot be undone.</p>`;

    document.getElementById('confirmModal-confirm-btn').addEventListener('click', async () => {
      modal.hide();
      if (deleteUserId) {
        const result = await sendFetch(null, `/api/delete-user/${deleteUserId}`, 'POST');
        if (result && result.status === 200) {
          document.getElementById('userRow' + deleteUserId).remove();
          createPopup(result.response, messageLevel.SUCCESS, alertTime);
        } else createPopup(result.response, messageLevel.ERROR, alertTime);
      }
      deleteUserId = null;
    });


  })

  function confirmDelete(userId) {
    deleteUserId = userId;
    modal.show();
  }

  async function updateRole(userId, newRole) {
    let result;

    try {
      result = await sendFetch({role: newRole}, '/api/update-role/' + userId, 'post');
    } catch (e) {
      createPopup(e, messageLevel.ERROR, alertTime);
      result = {};
    }

    let select = document.getElementById('roleId' + userId);
    if (result.status === 200) {
      let defaultRemoved = false;
      let defaultAdded = false;
      for (const option of select.children) {
        if (option.default === 'default') {
          defaultRemoved = true;
          option.default = false;
          option.removeAttribute('default');
        } else if (option.value === newRole) {
          defaultAdded = true
          option.default = 'default';
        }
        if (defaultRemoved && defaultAdded) break;
      }
      createPopup(result.response, messageLevel.SUCCESS, alertTime);
    } else {
      for (const option of select.children) {
        if (option.default === 'default') {
          let onchange = select.onchange;
          select.onchange = null;
          select.value = result.role || option.value;
          select.onchange = onchange;
          break;
        }
      }
      if (result !== {})
        createPopup(result.response, messageLevel.ERROR, alertTime);
    }
  }

  return {
    confirmDelete,
    updateRole,
  }

})();