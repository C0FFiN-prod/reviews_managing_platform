(() => {
  let imgX = 0, imgY = 0;
  let isDragging = false;
  let dragStartX, dragStartY;
  let scale = 1;
  let preScale = 1;
  let canvas;
  let ctx;
  let scaleInput;
  let upload;
  let img = new Image();
  document.addEventListener("DOMContentLoaded", async () => {
    upload = document.getElementById('uploadAvatar');
    document.getElementById('uploadAvatarBtn').addEventListener('click', () => upload.click());
    canvas = document.getElementById('canvasAvatar');
    ctx = canvas.getContext('2d');
    scaleInput = document.getElementById('scaleAvatar');
    const avatarModal = document.getElementById('avatarModal')
    avatarModal.addEventListener("shown.bs.modal", () => drawImage());
    avatarModal.addEventListener("hidden.bs.modal", () => {
      img.removeAttribute('src');
      ctx.fillRect(0, 0, canvas.width, canvas.height);
      scaleInput.value = 1;
    })
    upload.addEventListener('change', (e) => {
      processFile(e.target.files[0], 1000)
        .then(imgSrc => {
          img.src = imgSrc;
          img.onload = () => {
            preScale = 1000 / img.height;
            scale = preScale;
            scaleInput.value = 1;
            scaleInput.dispatchEvent(new Event('input'));
            canvas.mousedown();
            drawImage();
          };
        });
    });
    scaleInput.addEventListener('input', (e) => {
      const oldScale = scale;
      const canvasSize = canvas.width;
      const minScale = Math.max(canvasSize / img.width, canvasSize / img.height);
      const newScale = Math.max(parseFloat(e.target.value) * preScale, minScale);

      const newWidth = img.width * newScale;
      const newHeight = img.height * newScale;

      scaleInput.value = newScale / preScale;

      const currentImgCenterX = imgX + (img.width * oldScale) / 2;
      const currentImgCenterY = imgY + (img.height * oldScale) / 2;
      const newImgX = currentImgCenterX - (newWidth / 2);
      const newImgY = currentImgCenterY - (newHeight / 2);
      scale = newScale;
      imgX = newImgX;
      imgY = newImgY;
      imgX = Math.min(Math.max(imgX, canvasSize - newWidth), 0);
      imgY = Math.min(Math.max(imgY, canvasSize - newHeight), 0);
      drawImage();
    });
    canvas.addEventListener('selectstart', (e) => {
      e.preventDefault();
    });
    canvas.addEventListener('mousedown', (e) => {
      e.preventDefault();
      updateCanvasSize(canvas);
      const x = e.clientX;
      const y = e.clientY;
      if (x >= 0 && x <= canvas.width && y >= 0 && y <= canvas.width) {
        isDragging = true;
        dragStartX = x;
        dragStartY = y;
      }
    });
    canvas.addEventListener('mousemove', (e) => {
      if (isDragging) {
        e.preventDefault();
        updateCanvasSize(canvas);
        imgX += (e.clientX - dragStartX) * 2;
        imgY += (e.clientY - dragStartY) * 2;
        dragStartX = e.clientX;
        dragStartY = e.clientY;
        imgX = Math.min(Math.max(imgX, canvas.width - img.width * scale), 0);
        imgY = Math.min(Math.max(imgY, canvas.height - img.height * scale), 0);
        drawImage();
      }
    });
    canvas.addEventListener('mouseup', () => {
      isDragging = false;
    });
    canvas.addEventListener('mouseleave', () => {
      isDragging = false;
    });
    document.getElementById('saveAvatarBtn').addEventListener('click', async () => {
      if (!img.src) return;

      const tempCanvas = document.createElement('canvas');
      const resultSize = 200;
      tempCanvas.width = resultSize;
      tempCanvas.height = resultSize;
      const tempCtx = tempCanvas.getContext('2d');
      const factor = resultSize / canvas.width;
      tempCtx.drawImage(img,
        imgX * factor,
        imgY * factor,
        img.width * scale * factor,
        img.height * scale * factor
      );
      const croppedImage = tempCanvas.toDataURL("image/jpeg", 0.9);

      try {
        const formData = new FormData();
        formData.append('avatar', DataURIToBlob(croppedImage), 'avatar.jpg');

        const response = await sendFetch(
          formData,
          '/api/update-avatar',
          'POST'
        );

        if (response.ok) {
          const userData = JSON.parse(sessionStorage.getItem('currentUser'));
          if (userData) {
            userData.avatarPath = response.avatarPath;
            sessionStorage.setItem('currentUser', JSON.stringify(userData));
          }
          UserModule.updateProfile();
          const profileAvatar = document.getElementById('profileAvatar');
          if (profileAvatar) {
            const avatar = document.createElement('img');
            avatar.src = '/images/' + response.avatarPath;
            avatar.className = 'avatar-lg rounded-2 gallery-item';
            avatar.id = 'profileAvatar';
            profileAvatar.replaceWith(avatar);
          }

          // Close modal after successful upload
          const modal = bootstrap.Modal.getInstance(document.getElementById('avatarModal'));
          modal.hide();

          createPopup("Avatar updated successfully!", messageLevel.SUCCESS);
        } else {
          createPopup(response.response, messageLevel.ERROR);
        }
      } catch (error) {
        createPopup("Error uploading avatar: " + error, messageLevel.ERROR);
      }
    });
  });

  function updateCanvasSize(canvas) {
    const realWidth = canvas.clientWidth;
    const realHeight = canvas.clientHeight;
    if (10 < Math.abs(canvas.height - realHeight * 2))
      canvas.height = realHeight * 2;
    if (10 < Math.abs(canvas.width - realWidth * 2))
      canvas.width = realWidth * 2;
  }

  function drawImage() {
    updateCanvasSize(canvas);
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    if (!img.src) {
      ctx.save();
      ctx.fillStyle = '#f0f0f0';
      ctx.fillRect(0, 0, canvas.width, canvas.height);
      ctx.fillStyle = '#888888';
      ctx.font = '32px Arial';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText('Upload new avatar', canvas.width / 2, canvas.height / 2);
    } else {
      const canvasSize = canvas.width;
      const circleRadius = canvasSize / 2;
      const circleX = canvasSize / 2;
      const circleY = canvasSize / 2;
      ctx.save();
      ctx.drawImage(img, imgX, imgY, img.width * scale, img.height * scale);
      ctx.globalCompositeOperation = 'source-over';
      ctx.fillStyle = 'rgba(0, 0, 0, 0.5)';
      ctx.beginPath();
      ctx.rect(0, 0, canvas.width, canvas.height);
      ctx.arc(circleX, circleY, circleRadius, 0, Math.PI * 2, true);
      ctx.fill();
      ctx.strokeStyle = '#ffffff';
      ctx.lineWidth = 2;
      ctx.beginPath();
      ctx.arc(circleX, circleY, circleRadius, 0, Math.PI * 2, true);
      ctx.stroke();
    }
    ctx.restore();
  }
})();
