package com.javarush.taskmanager.project.member;

import com.javarush.taskmanager.exception.ApiError;
import com.javarush.taskmanager.project.member.dto.AddMemberRequest;
import com.javarush.taskmanager.project.member.dto.MemberResponse;
import com.javarush.taskmanager.project.member.dto.UpdateMemberRoleRequest;
import com.javarush.taskmanager.security.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "Project Members",
        description = "Project membership management. All endpoints require a Bearer token obtained via /api/auth."
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/projects/{projectId}/members")
@RequiredArgsConstructor
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    @Operation(
            summary = "Add a member to the project",
            description = """
                    Adds an existing user to the project with the given role. Requires MANAGER
                    role or higher. OWNER role cannot be granted through this endpoint.
                    """
    )
    @ApiResponse(responseCode = "201", description = "Member added",
            content = @Content(schema = @Schema(implementation = MemberResponse.class)))
    @ApiResponse(responseCode = "400", description = "Validation error, or attempt to grant OWNER role",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "Access token is missing or invalid",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "The requester lacks the required role",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "The project does not exist, or no user with the given email was found",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "The user is already a member of this project",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @PostMapping
    public ResponseEntity<MemberResponse> addMember(
            @PathVariable UUID projectId,
            @Parameter(hidden = true) @CurrentUserId UUID requesterId,
            @Valid @RequestBody AddMemberRequest request) {
        MemberResponse response = projectMemberService.addMember(projectId, requesterId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "List project members",
            description = "Returns all members of the project. Available only to project members."
    )
    @ApiResponse(responseCode = "200", description = "List of members",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = MemberResponse.class))))
    @ApiResponse(responseCode = "401", description = "Access token is missing or invalid",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "The user is not a member of this project",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "A project with this id does not exist",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @GetMapping
    public ResponseEntity<List<MemberResponse>> getAllMembers(
            @PathVariable UUID projectId,
            @Parameter(hidden = true) @CurrentUserId UUID requesterId) {
        return ResponseEntity.ok(projectMemberService.getAllMembers(projectId, requesterId));
    }

    @Operation(
            summary = "Update a member's role",
            description = """
                    Changes the role of an existing project member. Requires OWNER role.
                    Ownership cannot be transferred through this endpoint, and the owner's role cannot be changed.
                    """
    )
    @ApiResponse(responseCode = "200", description = "Role updated",
            content = @Content(schema = @Schema(implementation = MemberResponse.class)))
    @ApiResponse(responseCode = "400", description = "Validation error, attempt to transfer ownership, or attempt to change the owner's role",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "Access token is missing or invalid",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "The requester lacks the required role",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "The project does not exist, or the member does not belong to it",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @PatchMapping("/{memberId}")
    public ResponseEntity<MemberResponse> updateMemberRole(
            @PathVariable UUID projectId,
            @PathVariable UUID memberId,
            @Parameter(hidden = true) @CurrentUserId UUID requesterId,
            @Valid @RequestBody UpdateMemberRoleRequest request) {
        MemberResponse response = projectMemberService.updateMemberRole(projectId, requesterId, memberId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Remove a member from the project",
            description = """
                    Removes a member from the project. Requires MANAGER
                    role or higher. The project owner cannot be removed.
                    """
    )
    @ApiResponse(responseCode = "204", description = "Member removed")
    @ApiResponse(responseCode = "400", description = "Attempt to remove the project owner",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "Access token is missing or invalid",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "The requester lacks the required role",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "The project does not exist, or the member does not belong to it",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID projectId,
            @PathVariable UUID memberId,
            @Parameter(hidden = true) @CurrentUserId UUID requesterId) {
        projectMemberService.removeMember(projectId, requesterId, memberId);
        return ResponseEntity.noContent().build();
    }
}