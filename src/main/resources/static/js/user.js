const UserModule = (() => {
  let username;
  let registrationDate;
  let reviewCount;
  let email;
  let visibleName;
  let phone;
  let userData;
  let profileChanged = false;
  document.addEventListener("utilsInitiated", () => {
    document.getElementById("deleteSelfBtn").addEventListener("click", deleteSelf);
    username = document.getElementById('username');
    registrationDate = document.getElementById('registrationDate');
    reviewCount = document.getElementById('reviewCount');
    email = document.getElementById('email');
    visibleName = document.getElementById('visibleName');
    phone = document.getElementById('phone');
    updateProfile();
    const saveButton = document.getElementById('saveProfileChangesBtn');
    const emailInput = document.getElementById('email');
    const phoneInput = document.getElementById('phone');
    const visibleNameInput = document.getElementById('visibleName');

    function checkChanges() {
      const userData = JSON.parse(sessionStorage.getItem('currentUser'));
      const hasChanges =
        emailInput.value !== userData.email ||
        phoneInput.value !== userData.phone ||
        visibleNameInput.value !== userData.visibleName;

      saveButton.classList.toggle('disabled', !hasChanges);
      saveButton.classList.toggle('btn-secondary', !hasChanges);
      saveButton.classList.toggle('btn-primary', hasChanges);
    }

    [emailInput, phoneInput, visibleNameInput].forEach(input => {
      input.addEventListener('input', checkChanges);
    });

    saveButton.addEventListener('click', async () => {
      const formData = new FormData();
      formData.append('email', emailInput.value);
      formData.append('phone', phoneInput.value);
      formData.append('visibleName', visibleNameInput.value);

      try {
        const response = await sendFetch(formData, '/api/update-profile', 'POST');
        if (response.ok) {
          if (response.changed)
            sessionStorage.setItem('currentUser', JSON.stringify(response.userData));
          createPopup('Profile updated successfully', messageLevel.SUCCESS);
          updateProfile();
        } else {
          createPopup(response.response, messageLevel.ERROR);
        }
      } catch (error) {
        createPopup('Error updating profile: ' + error, messageLevel.ERROR);
      }
    });
    loadBookmarks().then(_ => {
    });
    loadMyReviews().then(_ => {
    });
  })

  function updateProfile() {
    userData = JSON.parse(sessionStorage.getItem('currentUser'));
    username.innerHTML = userData.username;
    registrationDate.innerHTML = formatDateTime(userData.registrationDate);
    email.value = userData.email ?? '';
    visibleName.value = userData.visibleName ?? userData.username;
    phone.value = userData.phoneNumber ?? '';
    profileChanged = false;
    let currentAvatar = document.getElementById('profileAvatar');
    let avatar;
    if (userData.avatarPath) {
      avatar = document.createElement('img');
      avatar.src = '/images/' + userData.avatarPath;
      avatar.className = 'avatar-lg rounded-2';
    } else {
      avatar = generateAvatar(
        userData.visibleName ?? userData.username,
        'avatar-lg', false);
    }
    avatar.id = 'profileAvatar';
    currentAvatar.replaceWith(avatar);
    currentAvatar = document.getElementById('headerUserAvatar');
    if (userData.avatarPath) {
      avatar = document.createElement('img');
      avatar.src = '/images/' + userData.avatarPath;
      avatar.className = 'avatar-xs rounded-circle';
    } else {
      avatar = generateAvatar(
        userData.visibleName ?? userData.username,
        'avatar-xs', true);
    }
    avatar.id = 'headerUserAvatar';
    currentAvatar.replaceWith(avatar);
  }

  async function deleteSelf(event) {
    event.preventDefault();
    let handleDelete = async () => {
      const result = await sendFetch(null, '/api/delete-user/' + userId, 'post');
      if (result && result.status === 200) {
        window.location.href = "/login?delete";
      } else createPopup(result.response, messageLevel.ERROR, alertTime);
    }
    modalConfirmBtn.addEventListener('click', handleDelete, {once: true});
    modalConfirm.addEventListener('hidden.bs.modal', () => {
      modalConfirmBtn.removeEventListener('click', handleDelete);
    });
  }

  async function loadBookmarks() {
    const response = await fetch('/api/get-bookmarks');
    const bookmarks = await response.json();
    const bookmarksList = document.getElementById('bookmarksList');
    bookmarksList.innerHTML = bookmarks.map(bookmark => `
    <li class="list-group-item flex-vcenter">
    <button class="text-info text-warning btn btn-square cursor-pointer me-1" 
    onclick="toggleBookmark(this,${bookmark.reviewId})"><i class="bi bi-bookmark-fill"></i></button>
      <a href="/review/${bookmark.reviewId}" class="text-dark text-decoration-none">${bookmark.reviewTitle}</a>
    </li>
  `).join('') || `<h5 class="list-group-item m-0">You have no bookmarks</h5>`;
  }

  async function loadMyReviews() {
    try {
      const response = await fetch(`/api/public/get-reviews/${userId}?published=false`);
      if (response.ok && !response.redirected) {
        const data = await response.json();
        const myReviewsList = document.getElementById('myReviewsList');
        reviewCount.innerHTML = `${formatLikes(data.totalElements)}`
        myReviewsList.innerHTML = data.reviews.map(review => {
          let status = '<span class="text-';
          switch (review.status) {
            case 'PUBLISHED':
              status += `success text-opacity-75"><i class="bi bi-book-fill me-1"></i>`;
              break;
            case 'MODERATION':
              status += `warning text-opacity-75"><i class="bi bi-eyeglasses me-1"></i>`;
              break;
            case 'DRAFT':
              status += `info text-opacity-75"><i class="bi bi-book-half me-1"></i>`;
              break;
            case 'HIDDEN':
              status += `info text-opacity-50"><i class="bi bi-eye-slash-fill me-1"></i>`;
              break;
          }
          status += review.status + '</span>';

          return `
            <li class="list-group-item flex-row justify-content-between flex-vcenter">
              <div class="d-flex flex-column justify-content-between">
                <a href="/review/${review.id}" class="text-dark text-decoration-none d-none d-md-inline">${review.title}</a>
                <a href="/review/${review.id}" class="text-dark text-decoration-none d-inline d-md-none">${review.model}</a>
                <span class="d-flex justify-content-between date gap-1">
                  <span>${formatDateTime(review.createdAt)}</span>
                  <span>${review.likes}<i class="align-middle bi bi-heart-fill lh-1"></i></span>
                </span>
              </div>
              <div>
              ${status}
                  <span class="star-rating">${'<i class="bi bi-star-fill star-off"></i>'.repeat(5 - review.rating)}
                  ${'<i class="bi bi-star-fill star-on"></i>'.repeat(review.rating)}</span>
              </div>
              <button class="btn btn-outline-secondary btn-square" onclick="location.href='/edit-review/${review.id}'">
                <i class="bi bi-pencil-square"></i>
              </button>
            </li>
          `
        }).join('') || `<h5 class="list-group-item m-0">You have no reviews</h5>`;
      } else if (!response.ok && !response.redirected) {
        const data = await response.json();
        createPopup(data.response, messageLevel.WARNING);
      }
    } catch (e) {
      createPopup(e, messageLevel.ERROR);
    }


  }

  return {
    updateProfile
  }
})();