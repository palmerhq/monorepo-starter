package com.mono.api.access

import com.mono.api.auth.UserAuth
import com.mono.api.data.entity.Job
import com.mono.api.data.entity.PartialJob
import io.stardog.stardao.core.Update

class JobAccessControl: AbstractAccessControl<Job,PartialJob>() {
    override fun toVisible(auth: UserAuth?, model: Job): PartialJob {
        // if customer access, can read everything
        if (auth?.user?.isSuperuser() == true || hasCustomerAccess(auth, model.customerId)) {
            return PartialJob(model)
        } else {
            return PartialJob(
                    id = model.id,
                    companyName = model.companyName,
                    title = model.title,
                    status = model.status,
                    type = model.type,
                    category = model.category,
                    companyUrl = model.companyUrl,
                    applyUrl  = model.applyUrl,
                    remoteFriendly = model.remoteFriendly,
                    location = model.location,
                    imagePath = model.imagePath
            )
        }
    }

    override fun canRead(auth: UserAuth?, model: Job): Boolean {
        if (auth?.user?.isSuperuser() == true || hasCustomerAccess(auth, model.customerId)) {
            return true
        }
        return model.status == Job.Status.ACTIVE && model.deleteAt == null
    }

    override fun canCreate(auth: UserAuth, create: PartialJob): Boolean {
        return hasCustomerAccess(auth, create.customerId ?: return false)
    }

    override fun canUpdate(auth: UserAuth, model: Job, update: Update<PartialJob>): Boolean {
        return hasCustomerAccess(auth, model.customerId)
    }

    override fun canDelete(auth: UserAuth, model: Job): Boolean {
        return hasCustomerAccess(auth, model.customerId)
    }
}